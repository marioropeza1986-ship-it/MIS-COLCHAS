package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ColchasRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Debes iniciar sesión con Google primero.")
    }

    fun observeProductos(userId: String): Flow<List<Producto>> = flow {
        val uid = userId.takeIf { it.isNotBlank() } ?: requireUserId()
        val path = "users/$uid/productos"
        emitAll(
            db.collection("users").document(uid).collection("productos")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Producto::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeEntradas(userId: String): Flow<List<EntradaStock>> = flow {
        val uid = userId.takeIf { it.isNotBlank() } ?: requireUserId()
        val path = "users/$uid/entradas"
        emitAll(
            db.collection("users").document(uid).collection("entradas")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        EntradaStock::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeVentas(userId: String): Flow<List<Venta>> = flow {
        val uid = userId.takeIf { it.isNotBlank() } ?: requireUserId()
        val path = "users/$uid/ventas"
        emitAll(
            db.collection("users").document(uid).collection("ventas")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Venta::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addProducto(
        sku: String,
        modelo: String,
        tamano: String,
        color: String,
        ubicacion: String,
        fotoBase64: String?,
        stockInicial: Long,
        costo: Double,
        precio: Double,
        stockMinimo: Long,
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/productos"
        return try {
            val uid = requireUserId()
            val cleanModelo = modelo.trim().take(100)
            require(cleanModelo.isNotEmpty()) { "Escribe el nombre del modelo." }
            val cleanSku = sku.trim().uppercase().take(40).ifEmpty {
                "COL-${System.currentTimeMillis().toString().takeLast(4)}"
            }
            val cleanTamano = tamano.trim().take(40).ifEmpty { "Matrimonial" }
            val cleanColor = color.trim().take(60)
            val cleanUbicacion = ubicacion.trim().take(60).ifEmpty { "Estante / Tienda" }
            val cleanFoto = fotoBase64?.takeIf { it.isNotBlank() && it.length <= 80000 }

            val docRef = db.collection("users").document(uid).collection("productos").document()
            docPath = docRef.path

            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "sku" to cleanSku,
                "modelo" to cleanModelo,
                "tamano" to cleanTamano,
                "color" to cleanColor,
                "ubicacion" to cleanUbicacion,
                "stockInicial" to stockInicial.coerceAtLeast(0L),
                "costo" to costo.coerceAtLeast(0.0),
                "precio" to precio.coerceAtLeast(0.0),
                "stockMinimo" to stockMinimo.coerceAtLeast(0L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (cleanFoto != null) {
                payload["fotoBase64"] = cleanFoto
            }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProducto(
        productoId: String,
        sku: String,
        modelo: String,
        tamano: String,
        color: String,
        ubicacion: String,
        fotoBase64: String?,
        stockInicial: Long,
        costo: Double,
        precio: Double,
        stockMinimo: Long,
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/productos/$productoId"
        return try {
            val uid = requireUserId()
            require(productoId.isNotBlank()) { "Falta el identificador de la colcha." }
            val cleanModelo = modelo.trim().take(100)
            require(cleanModelo.isNotEmpty()) { "Escribe el nombre del modelo." }

            val docRef = db.collection("users").document(uid).collection("productos").document(productoId)
            docPath = docRef.path

            val updates = mutableMapOf<String, Any>(
                "sku" to sku.trim().uppercase().take(40).ifEmpty { "COL-001" },
                "modelo" to cleanModelo,
                "tamano" to tamano.trim().take(40).ifEmpty { "Matrimonial" },
                "color" to color.trim().take(60),
                "ubicacion" to ubicacion.trim().take(60).ifEmpty { "Estante / Tienda" },
                "stockInicial" to stockInicial.coerceAtLeast(0L),
                "costo" to costo.coerceAtLeast(0.0),
                "precio" to precio.coerceAtLeast(0.0),
                "stockMinimo" to stockMinimo.coerceAtLeast(0L),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (fotoBase64 != null && fotoBase64.length <= 80000) {
                updates["fotoBase64"] = fotoBase64
            }

            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUbicacion(productoId: String, nuevaUbicacion: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/productos/$productoId"
        return try {
            val uid = requireUserId()
            require(productoId.isNotBlank()) { "Falta el identificador de la colcha." }
            val docRef = db.collection("users").document(uid).collection("productos").document(productoId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "ubicacion" to nuevaUbicacion.trim().take(60).ifEmpty { "Estante / Tienda" },
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProducto(productoId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/productos/$productoId"
        return try {
            val uid = requireUserId()
            require(productoId.isNotBlank()) { "Falta el identificador de la colcha." }
            val docRef = db.collection("users").document(uid).collection("productos").document(productoId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addEntrada(
        fecha: String,
        producto: Producto,
        cantidad: Long,
        costoUnitario: Double,
        proveedor: String,
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/entradas"
        return try {
            val uid = requireUserId()
            require(producto.id.isNotBlank()) { "Selecciona un modelo válido." }
            require(cantidad >= 1L) { "La cantidad debe ser al menos 1." }

            val userDoc = db.collection("users").document(uid)
            val prodRef = userDoc.collection("productos").document(producto.id)
            val entRef = userDoc.collection("entradas").document()
            docPath = entRef.path

            val cleanCosto = costoUnitario.coerceAtLeast(0.0)
            val entradaPayload = mapOf<String, Any>(
                "userId" to uid,
                "fecha" to fecha.trim().take(20),
                "productoId" to producto.id,
                "sku" to producto.sku.take(40),
                "modelo" to producto.modelo.take(100),
                "cantidad" to cantidad,
                "costo" to cleanCosto,
                "proveedor" to proveedor.trim().take(100),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )

            val batch = db.batch()
            batch.set(entRef, entradaPayload)
            batch.update(
                prodRef,
                mapOf(
                    "costo" to cleanCosto,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addVenta(
        fecha: String,
        producto: Producto,
        cantidad: Long,
        precioUnitario: Double,
        modalidad: String,
        enganche: Double,
        proximoCobro: String,
        clienteNombre: String,
        clienteTelefono: String,
        nota: String,
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/ventas"
        return try {
            val uid = requireUserId()
            require(producto.id.isNotBlank()) { "Selecciona una colcha válida." }
            require(cantidad >= 1L) { "La cantidad debe ser al menos 1." }
            val cleanCliente = clienteNombre.trim().take(100)
            require(cleanCliente.isNotEmpty()) { "Escribe el nombre de la clienta." }

            val cleanFecha = fecha.trim().take(20)
            val cleanPrecio = precioUnitario.coerceAtLeast(0.0)
            val importeTotal = cleanPrecio * cantidad
            val esPagos = modalidad == "pagos"
            val pagado = if (esPagos) enganche.coerceIn(0.0, importeTotal) else importeTotal
            val saldo = (importeTotal - pagado).coerceAtLeast(0.0)
            val cleanProximoCobro = if (saldo > 0.0) proximoCobro.trim().take(20) else ""

            val historial = buildList {
                if (esPagos) {
                    if (pagado > 0.0) {
                        add(mapOf("fecha" to cleanFecha, "monto" to pagado, "concepto" to "Enganche"))
                    }
                } else {
                    add(mapOf("fecha" to cleanFecha, "monto" to importeTotal, "concepto" to "Pago de contado"))
                }
            }

            val docRef = db.collection("users").document(uid).collection("ventas").document()
            docPath = docRef.path

            val payload = mapOf<String, Any>(
                "userId" to uid,
                "fecha" to cleanFecha,
                "productoId" to producto.id,
                "sku" to producto.sku.take(40),
                "modelo" to producto.modelo.take(100),
                "cantidad" to cantidad,
                "precioUnitario" to cleanPrecio,
                "costoUnitario" to producto.costo.coerceAtLeast(0.0),
                "importeTotal" to importeTotal,
                "pagado" to pagado,
                "saldo" to saldo,
                "modalidad" to if (esPagos) "pagos" else "contado",
                "proximoCobro" to cleanProximoCobro,
                "clienteNombre" to cleanCliente,
                "clienteTelefono" to clienteTelefono.trim().take(40),
                "nota" to nota.trim().take(200),
                "historialPagos" to historial,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun registrarAbono(
        venta: Venta,
        montoAbono: Double,
        fechaHoy: String,
        nuevaFechaCobro: String,
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/ventas/${venta.id}"
        return try {
            val uid = requireUserId()
            require(venta.id.isNotBlank()) { "Falta el identificador de la venta." }
            val saldoActual = venta.saldo.coerceAtLeast(0.0)
            val abonoSeguro = montoAbono.coerceIn(0.0, saldoActual)
            require(abonoSeguro > 0.0) { "El abono debe ser mayor a $0." }

            val nuevoPagado = venta.pagado + abonoSeguro
            val nuevoSaldo = (saldoActual - abonoSeguro).coerceAtLeast(0.0)
            val proximo = if (nuevoSaldo > 0.0) nuevaFechaCobro.trim().take(20) else ""

            val historialActualizado = (venta.historialPagos + PagoRegistro(
                fecha = fechaHoy.trim().take(20),
                monto = abonoSeguro,
                concepto = if (nuevoSaldo <= 0.0) "Liquidación" else "Abono",
            )).takeLast(100).map {
                mapOf(
                    "fecha" to it.fecha,
                    "monto" to it.monto,
                    "concepto" to it.concepto,
                )
            }

            val docRef = db.collection("users").document(uid).collection("ventas").document(venta.id)
            docPath = docRef.path

            docRef.update(
                mapOf(
                    "pagado" to nuevoPagado,
                    "saldo" to nuevoSaldo,
                    "proximoCobro" to proximo,
                    "historialPagos" to historialActualizado,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteVenta(ventaId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/ventas/$ventaId"
        return try {
            val uid = requireUserId()
            require(ventaId.isNotBlank()) { "Falta el identificador de la venta." }
            val docRef = db.collection("users").document(uid).collection("ventas").document(ventaId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
