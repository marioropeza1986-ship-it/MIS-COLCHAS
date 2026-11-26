package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ColchaCatalogOptions
import com.example.data.Producto
import com.example.data.ProductoConStock
import com.example.ui.components.QuiltThumbnail
import com.example.ui.components.compressImageUriToBase64
import com.example.ui.components.todayIsoDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoFormSheet(
    existingProducto: ProductoConStock?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit,
    onDelete: ((Producto) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val p = existingProducto?.producto
    var modelo by remember { mutableStateOf(p?.modelo ?: "") }
    var cantidadText by remember {
        mutableStateOf((existingProducto?.disponibles ?: 1L).coerceAtLeast(0L).toString())
    }
    var precioText by remember {
        mutableStateOf(
            if (p != null && p.precio > 0) {
                if (p.precio % 1.0 == 0.0) p.precio.toInt().toString() else p.precio.toString()
            } else {
                ""
            }
        )
    }
    var costoText by remember {
        mutableStateOf(
            if (p != null && p.costo > 0) {
                if (p.costo % 1.0 == 0.0) p.costo.toInt().toString() else p.costo.toString()
            } else {
                ""
            }
        )
    }
    var tamano by remember { mutableStateOf(p?.tamano ?: "Matrimonial") }
    var ubicacion by remember { mutableStateOf(p?.ubicacion ?: "Estante / Tienda") }
    var color by remember { mutableStateOf(p?.color ?: "") }
    var sku by remember { mutableStateOf(p?.sku ?: "") }
    var stockMinText by remember { mutableStateOf((p?.stockMinimo ?: 2L).toString()) }
    var fotoBase64 by remember { mutableStateOf(p?.fotoBase64) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val compressed = compressImageUriToBase64(context, uri)
                if (compressed != null) {
                    fotoBase64 = compressed
                } else {
                    errorMsg = "No se pudo cargar esa foto. Intenta con otra imagen."
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (p == null) "Nueva colcha" else "Editar colcha",
                style = MaterialTheme.typography.headlineSmall,
            )

            // 1. Nombre, Cantidad y Precio arriba para registro en segundos
            OutlinedTextField(
                value = modelo,
                onValueChange = { modelo = it; errorMsg = null },
                label = { Text("Nombre de la colcha *") },
                placeholder = { Text("Ej. Colcha Primavera Floral") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("producto_modelo_input"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = cantidadText,
                    onValueChange = { cantidadText = it.filter { ch -> ch.isDigit() }; errorMsg = null },
                    label = { Text("Cantidad (piezas) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("producto_cantidad_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                OutlinedTextField(
                    value = precioText,
                    onValueChange = { precioText = it; errorMsg = null },
                    label = { Text("Precio venta ($) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("producto_precio_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
            }

            // Photo picker row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                QuiltThumbnail(
                    fotoBase64 = fotoBase64,
                    contentDescription = "Foto de la colcha",
                    size = 76.dp,
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (fotoBase64.isNullOrBlank()) "Agregar foto (opcional)" else "Cambiar foto")
                    }
                }
            }

            Text("Tamaño", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ColchaCatalogOptions.tamanos.forEach { t ->
                    FilterChip(
                        selected = tamano == t,
                        onClick = { tamano = t },
                        label = { Text(t) },
                        modifier = Modifier.height(44.dp),
                    )
                }
            }

            Text("¿Dónde está guardada?", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ColchaCatalogOptions.ubicaciones.forEach { loc ->
                    FilterChip(
                        selected = ubicacion == loc,
                        onClick = { ubicacion = loc },
                        label = { Text(loc) },
                        modifier = Modifier.height(44.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = costoText,
                    onValueChange = { costoText = it },
                    label = { Text("Costo ($) opcional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                OutlinedTextField(
                    value = stockMinText,
                    onValueChange = { stockMinText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Avisar si hay ≤") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
            }

            OutlinedTextField(
                value = color,
                onValueChange = { color = it },
                label = { Text("Color o diseño (opcional)") },
                placeholder = { Text("Ej. Vino / Rosa") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )

            OutlinedTextField(
                value = sku,
                onValueChange = { sku = it },
                label = { Text("Código (opcional, se crea solo)") },
                placeholder = { Text("Se genera automático si lo dejas vacío") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )

            if (errorMsg != null) {
                Text(
                    text = errorMsg ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (p != null && onDelete != null) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar colcha")
                    }
                }
                Button(
                    onClick = {
                        if (modelo.isBlank()) {
                            errorMsg = "Escribe el nombre de la colcha."
                            return@Button
                        }
                        val desiredDisp = cantidadText.toLongOrNull() ?: 0L
                        val computedStockInicial = if (existingProducto != null) {
                            (desiredDisp - existingProducto.totalEntradas + existingProducto.totalVendidas)
                                .coerceAtLeast(0L)
                        } else {
                            desiredDisp.coerceAtLeast(0L)
                        }
                        onSave(
                            sku,
                            modelo,
                            tamano,
                            color,
                            ubicacion,
                            fotoBase64,
                            computedStockInicial,
                            costoText.toDoubleOrNull() ?: 0.0,
                            precioText.toDoubleOrNull() ?: 0.0,
                            stockMinText.toLongOrNull() ?: 2L,
                        )
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("save_producto_button"),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = if (isBusy) "Guardando..." else "Guardar colcha",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDeleteConfirm && p != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar ${p.modelo}?") },
            text = { Text("Se quitará de tu catálogo de colchas.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(p)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Sí, eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReabastecerSheet(
    productos: List<ProductoConStock>,
    preselected: ProductoConStock?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSaveEntrada: (
        fecha: String,
        producto: Producto,
        cantidad: Long,
        costoUnitario: Double,
        proveedor: String,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember { mutableStateOf(preselected ?: productos.firstOrNull()) }
    var cantidad by remember { mutableLongStateOf(1L) }
    var costoText by remember(selected) {
        mutableStateOf(
            selected?.producto?.costo?.takeIf { it > 0 }?.toInt()?.toString() ?: ""
        )
    }
    var proveedor by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Surtir colchas (Entrada)", style = MaterialTheme.typography.headlineSmall)

            if (productos.isEmpty()) {
                Text(
                    text = "Primero agrega un modelo de colcha nuevo.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text("Elige el modelo:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    productos.forEach { item ->
                        FilterChip(
                            selected = selected?.producto?.id == item.producto.id,
                            onClick = { selected = item },
                            label = { Text("${item.producto.modelo} (${item.disponibles})") },
                            modifier = Modifier.height(44.dp),
                        )
                    }
                }

                Text("¿Cuántas piezas llegaron?", style = MaterialTheme.typography.labelLarge)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedButton(
                        onClick = { if (cantidad > 1L) cantidad-- },
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Restar 1")
                    }
                    Text(
                        text = "$cantidad",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    OutlinedButton(
                        onClick = { cantidad++ },
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Sumar 1")
                    }
                }

                OutlinedTextField(
                    value = costoText,
                    onValueChange = { costoText = it },
                    label = { Text("Costo por pieza ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )

                OutlinedTextField(
                    value = proveedor,
                    onValueChange = { proveedor = it },
                    label = { Text("Proveedor (opcional)") },
                    placeholder = { Text("Ej. Bodega Centro") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )

                Button(
                    onClick = {
                        val prod = selected?.producto ?: return@Button
                        onSaveEntrada(
                            todayIsoDate(),
                            prod,
                            cantidad,
                            costoText.toDoubleOrNull() ?: prod.costo,
                            proveedor,
                        )
                    },
                    enabled = !isBusy && selected != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = if (isBusy) "Guardando..." else "Sumar +$cantidad al inventario",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
