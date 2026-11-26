package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ColchasRepository
import com.example.data.EntradaStock
import com.example.data.PreferencesRepository
import com.example.data.Producto
import com.example.data.ProductoConStock
import com.example.data.ResumenTamano
import com.example.data.UiState
import com.example.data.Venta
import com.example.ui.components.todayIsoDate
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardData(
    val productosConStock: List<ProductoConStock> = emptyList(),
    val entradas: List<EntradaStock> = emptyList(),
    val ventas: List<Venta> = emptyList(),
    val totalPiezasDisponibles: Long = 0L,
    val valorCostoInventario: Double = 0.0,
    val porCobrarTotal: Double = 0.0,
    val gananciaBruta: Double = 0.0,
    val ventasPendientes: List<Venta> = emptyList(),
    val cobrosUrgentes: List<Venta> = emptyList(),
    val stockBajo: List<ProductoConStock> = emptyList(),
    val resumenPorTamano: List<ResumenTamano> = emptyList(),
    val clientasSugeridas: List<String> = emptyList(),
)

class ColchasViewModel(
    private val repository: ColchasRepository,
    private val preferencesRepository: PreferencesRepository,
    private val currentUserId: String,
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    val largeTextEnabled: StateFlow<Boolean> = preferencesRepository.largeTextEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val themeMode: StateFlow<String> = preferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "light")

    val visualStyle: StateFlow<String> = preferencesRepository.visualStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "terracota")

    val uiState: StateFlow<UiState<DashboardData>> = combine(
        repository.observeProductos(currentUserId),
        repository.observeEntradas(currentUserId),
        repository.observeVentas(currentUserId),
    ) { productos, entradas, ventas ->
        val entradasPorProd = entradas.groupBy { it.productoId }
        val ventasPorProd = ventas.groupBy { it.productoId }

        val productosConStock = productos
            .sortedBy { it.modelo.lowercase() }
            .map { p ->
                val sumEnt = entradasPorProd[p.id]?.sumOf { it.cantidad } ?: 0L
                val sumVen = ventasPorProd[p.id]?.sumOf { it.cantidad } ?: 0L
                val disp = (p.stockInicial + sumEnt - sumVen)
                ProductoConStock(
                    producto = p,
                    totalEntradas = sumEnt,
                    totalVendidas = sumVen,
                    disponibles = disp,
                )
            }

        val totalPiezas = productosConStock.sumOf { it.disponibles.coerceAtLeast(0L) }
        val valorCosto = productosConStock.sumOf {
            it.disponibles.coerceAtLeast(0L) * it.producto.costo
        }
        val porCobrar = ventas.sumOf { it.saldo.coerceAtLeast(0.0) }
        val ganancia = ventas.sumOf { (it.precioUnitario - it.costoUnitario) * it.cantidad }

        val ventasPendientes = ventas
            .filter { it.saldo > 0.0 }
            .sortedWith(
                compareBy<Venta> { it.proximoCobro.ifBlank { "9999-12-31" } }
                    .thenByDescending { it.saldo }
            )

        val hoy = todayIsoDate()
        val cobrosUrgentes = ventasPendientes.filter {
            it.proximoCobro.isNotBlank() && it.proximoCobro <= hoy
        }

        val stockBajo = productosConStock.filter {
            it.disponibles <= it.producto.stockMinimo
        }

        val resumenTamano = productosConStock
            .groupBy { it.producto.tamano.ifBlank { "Sin tamaño" } }
            .map { (tamano, lista) ->
                ResumenTamano(
                    tamano = tamano,
                    modelos = lista.size,
                    piezasDisponibles = lista.sumOf { it.disponibles.coerceAtLeast(0L) },
                    valorCosto = lista.sumOf { it.disponibles.coerceAtLeast(0L) * it.producto.costo },
                )
            }
            .sortedByDescending { it.piezasDisponibles }

        val clientas = ventas
            .map { it.clienteNombre.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()

        UiState.Success(
            DashboardData(
                productosConStock = productosConStock,
                entradas = entradas.sortedByDescending { it.fecha },
                ventas = ventas.sortedByDescending { it.fecha },
                totalPiezasDisponibles = totalPiezas,
                valorCostoInventario = valorCosto,
                porCobrarTotal = porCobrar,
                gananciaBruta = ganancia,
                ventasPendientes = ventasPendientes,
                cobrosUrgentes = cobrosUrgentes,
                stockBajo = stockBajo,
                resumenPorTamano = resumenTamano,
                clientasSugeridas = clientas,
            )
        ) as UiState<DashboardData>
    }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing colchas dashboard", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "No se pudieron cargar tus datos."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading,
        )

    fun setLargeTextEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setLargeTextEnabled(enabled)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setVisualStyle(style: String) {
        viewModelScope.launch {
            preferencesRepository.setVisualStyle(style)
        }
    }

    fun addProducto(
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
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.addProducto(
                sku = sku,
                modelo = modelo,
                tamano = tamano,
                color = color,
                ubicacion = ubicacion,
                fotoBase64 = fotoBase64,
                stockInicial = stockInicial,
                costo = costo,
                precio = precio,
                stockMinimo = stockMinimo,
            )
            _isBusy.value = false
            result.onSuccess {
                _toastEvents.tryEmit("Colcha \"$modelo\" guardada.")
                onSuccess()
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "No se pudo guardar la colcha.")
            }
        }
    }

    fun updateProducto(
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
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateProducto(
                productoId = productoId,
                sku = sku,
                modelo = modelo,
                tamano = tamano,
                color = color,
                ubicacion = ubicacion,
                fotoBase64 = fotoBase64,
                stockInicial = stockInicial,
                costo = costo,
                precio = precio,
                stockMinimo = stockMinimo,
            )
            _isBusy.value = false
            result.onSuccess {
                _toastEvents.tryEmit("Cambios guardados en \"$modelo\".")
                onSuccess()
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "No se pudo actualizar la colcha.")
            }
        }
    }

    fun updateUbicacion(producto: Producto, nuevaUbicacion: String) {
        viewModelScope.launch {
            val result = repository.updateUbicacion(producto.id, nuevaUbicacion)
            result.onSuccess {
                _toastEvents.tryEmit("${producto.modelo} ahora está en: $nuevaUbicacion")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "No se pudo cambiar la ubicación.")
            }
        }
    }

    fun deleteProducto(producto: Producto, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteProducto(producto.id)
            _isBusy.value = false
            result.onSuccess {
                _toastEvents.tryEmit("Colcha eliminada del catálogo.")
                onSuccess()
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "No se pudo eliminar.")
            }
        }
    }

    fun addEntrada(
        fecha: String,
        producto: Producto,
        cantidad: Long,
        costoUnitario: Double,
        proveedor: String,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.addEntrada(
                fecha = fecha,
                producto = producto,
                cantidad = cantidad,
                costoUnitario = costoUnitario,
                proveedor = proveedor,
            )
            _isBusy.value = false
            result.onSuccess {
                _toastEvents.tryEmit("Se sumaron +$cantidad piezas a ${producto.modelo}.")
                onSuccess()
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "No se pudo registrar la entrada.")
            }
        }
    }

    fun addVenta(
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
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.addVenta(
                fecha = fecha,
                producto = producto,
                cantidad = cantidad,
                precioUnitario = precioUnitario,
                modalidad = modalidad,
                enganche = enganche,
                proximoCobro = proximoCobro,
                clienteNombre = clienteNombre,
                clienteTelefono = clienteTelefono,
                nota = nota,
            )
            _isBusy.value = false
            result.onSuccess {
                val msg = if (modalidad == "pagos") {
                    "Venta en pagos guardada para $clienteNombre."
                } else {
                    "Venta de contado registrada."
                }
                _toastEvents.tryEmit(msg)
                onSuccess()
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "No se pudo registrar la venta.")
            }
        }
    }

    fun registrarAbono(
        venta: Venta,
        montoAbono: Double,
        nuevaFechaCobro: String,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.registrarAbono(
                venta = venta,
                montoAbono = montoAbono,
                fechaHoy = todayIsoDate(),
                nuevaFechaCobro = nuevaFechaCobro,
            )
            _isBusy.value = false
            result.onSuccess {
                val restante = (venta.saldo - montoAbono).coerceAtLeast(0.0)
                val msg = if (restante <= 0.0) {
                    "¡Cuenta de ${venta.clienteNombre} liquidada!"
                } else {
                    "Abono guardado. Queda por cobrar."
                }
                _toastEvents.tryEmit(msg)
                onSuccess()
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "No se pudo guardar el abono.")
            }
        }
    }

    fun deleteVenta(venta: Venta) {
        viewModelScope.launch {
            val result = repository.deleteVenta(venta.id)
            result.onSuccess {
                _toastEvents.tryEmit("Registro de venta eliminado.")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "No se pudo eliminar la venta.")
            }
        }
    }

    private companion object {
        const val TAG = "ColchasVM"
    }
}
