package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class Producto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sku: String = "",
    val modelo: String = "",
    val tamano: String = "Matrimonial",
    val color: String = "",
    val ubicacion: String = "Estante / Tienda",
    val fotoBase64: String? = null,
    val stockInicial: Long = 0L,
    val costo: Double = 0.0,
    val precio: Double = 0.0,
    val stockMinimo: Long = 2L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class EntradaStock(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fecha: String = "",
    val productoId: String = "",
    val sku: String = "",
    val modelo: String = "",
    val cantidad: Long = 1L,
    val costo: Double = 0.0,
    val proveedor: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PagoRegistro(
    val fecha: String = "",
    val monto: Double = 0.0,
    val concepto: String = "",
)

@Keep
@IgnoreExtraProperties
data class Venta(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fecha: String = "",
    val productoId: String = "",
    val sku: String = "",
    val modelo: String = "",
    val cantidad: Long = 1L,
    val precioUnitario: Double = 0.0,
    val costoUnitario: Double = 0.0,
    val importeTotal: Double = 0.0,
    val pagado: Double = 0.0,
    val saldo: Double = 0.0,
    val modalidad: String = "contado",
    val proximoCobro: String = "",
    val clienteNombre: String = "",
    val clienteTelefono: String = "",
    val nota: String = "",
    val historialPagos: List<PagoRegistro> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class ProductoConStock(
    val producto: Producto,
    val totalEntradas: Long,
    val totalVendidas: Long,
    val disponibles: Long,
)

data class ResumenTamano(
    val tamano: String,
    val modelos: Int,
    val piezasDisponibles: Long,
    val valorCosto: Double,
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

object ColchaCatalogOptions {
    val tamanos = listOf(
        "Individual",
        "Matrimonial",
        "Queen",
        "King Size",
        "Infantil",
    )

    val ubicaciones = listOf(
        "Estante / Tienda",
        "Bodega",
        "Con mi hermana",
        "Exhibidor principal",
    )
}
