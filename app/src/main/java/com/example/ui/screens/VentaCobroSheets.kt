package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.Producto
import com.example.data.ProductoConStock
import com.example.data.Venta
import com.example.ui.components.addDaysIsoDate
import com.example.ui.components.formatFriendlyDate
import com.example.ui.components.formatMoney
import com.example.ui.components.todayIsoDate
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaVentaSheet(
    productosConStock: List<ProductoConStock>,
    clientasSugeridas: List<String>,
    preselected: ProductoConStock?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSaveVenta: (
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
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val disponibles = remember(productosConStock) { productosConStock.filter { it.disponibles > 0L } }
    var selected by remember {
        mutableStateOf(
            preselected?.takeIf { it.disponibles > 0L } ?: disponibles.firstOrNull()
        )
    }
    var cantidad by remember { mutableLongStateOf(1L) }
    var precioText by remember(selected) {
        mutableStateOf(selected?.producto?.precio?.takeIf { it > 0 }?.toInt()?.toString() ?: "")
    }
    var clienteNombre by remember { mutableStateOf("") }
    var clienteTelefono by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }
    var modalidad by remember { mutableStateOf("contado") }
    var engancheText by remember { mutableStateOf("0") }
    var proximoCobro by remember { mutableStateOf(addDaysIsoDate(todayIsoDate(), 7)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val precioNum = precioText.toDoubleOrNull() ?: 0.0
    val total = precioNum * cantidad
    val engancheNum = (engancheText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, total)
    val resta = (total - engancheNum).coerceAtLeast(0.0)

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
            Text("Registrar venta", style = MaterialTheme.typography.headlineSmall)

            if (disponibles.isEmpty()) {
                Text(
                    text = "No tienes colchas con piezas disponibles para vender. Primero agrega o surte un modelo.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text("1. ¿Qué colcha vendiste?", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    disponibles.forEach { item ->
                        FilterChip(
                            selected = selected?.producto?.id == item.producto.id,
                            onClick = {
                                selected = item
                                if (cantidad > item.disponibles) cantidad = item.disponibles
                            },
                            label = { Text("${item.producto.modelo} (${item.disponibles})") },
                            modifier = Modifier.height(44.dp),
                        )
                    }
                }

                val maxStock = selected?.disponibles ?: 1L
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("2. Cantidad", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Disponibles: $maxStock",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { if (cantidad > 1L) cantidad-- },
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Menos")
                        }
                        Text("$cantidad", style = MaterialTheme.typography.headlineSmall)
                        OutlinedButton(
                            onClick = { if (cantidad < maxStock) cantidad++ },
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Más")
                        }
                    }
                }

                OutlinedTextField(
                    value = precioText,
                    onValueChange = { precioText = it },
                    label = { Text("Precio por colcha ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )

                Text("3. ¿A quién se la vendiste?", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = clienteNombre,
                    onValueChange = { clienteNombre = it; errorMsg = null },
                    label = { Text("Nombre de la clienta *") },
                    placeholder = { Text("Ej. Sra. Rosa Martínez") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("venta_cliente_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )

                if (clientasSugeridas.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        clientasSugeridas.take(8).forEach { nombre ->
                            FilterChip(
                                selected = clienteNombre == nombre,
                                onClick = { clienteNombre = nombre; errorMsg = null },
                                label = { Text(nombre) },
                                modifier = Modifier.height(40.dp),
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = clienteTelefono,
                    onValueChange = { clienteTelefono = it },
                    label = { Text("Teléfono / WhatsApp (opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )

                Text("4. ¿Cómo va a pagar?", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("contado" to "De contado", "pagos" to "En abonos").forEach { (mod, label) ->
                        val active = modalidad == mod
                        Button(
                            onClick = { modalidad = mod },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = if (active) {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                        ) {
                            Text(label)
                        }
                    }
                }

                if (modalidad == "pagos") {
                    OutlinedTextField(
                        value = engancheText,
                        onValueChange = { engancheText = it },
                        label = { Text("¿Cuánto dio de enganche hoy? ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("Total: ${formatMoney(total)} − Enganche: ${formatMoney(engancheNum)}")
                        Text(
                            text = "Queda debiendo: ${formatMoney(resta)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (resta > 0) CrimsonError else EmeraldSuccess,
                        )
                    }

                    if (resta > 0) {
                        Text(
                            text = "Próximo cobro: ${formatFriendlyDate(proximoCobro)}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        QuickDateButtonsRow(
                            selectedIso = proximoCobro,
                            onSelectIso = { proximoCobro = it },
                            onOpenCalendar = { showDatePicker = true },
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                    ) {
                        Text(
                            text = "Total pagado de contado: ${formatMoney(total)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldSuccess,
                        )
                    }
                }

                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Nota o dirección (opcional)") },
                    placeholder = { Text("Ej. Abona los sábados") },
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

                Button(
                    onClick = {
                        val prod = selected?.producto ?: return@Button
                        if (clienteNombre.isBlank()) {
                            errorMsg = "Escribe el nombre de la clienta."
                            return@Button
                        }
                        onSaveVenta(
                            todayIsoDate(),
                            prod,
                            cantidad,
                            precioNum,
                            modalidad,
                            engancheNum,
                            if (modalidad == "pagos" && resta > 0) proximoCobro else "",
                            clienteNombre,
                            clienteTelefono,
                            nota,
                        )
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_venta_button"),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = if (isBusy) "Guardando..." else "Guardar venta",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        IsoDatePickerDialog(
            onDismiss = { showDatePicker = false },
            onConfirmIso = {
                proximoCobro = it
                showDatePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarAbonoSheet(
    venta: Venta,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSaveAbono: (montoAbono: Double, nuevaFechaCobro: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var montoText by remember { mutableStateOf("") }
    var proximaFecha by remember { mutableStateOf(addDaysIsoDate(todayIsoDate(), 7)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val montoNum = (montoText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, venta.saldo)
    val nuevoSaldo = (venta.saldo - montoNum).coerceAtLeast(0.0)

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
            Text("Registrar abono", style = MaterialTheme.typography.headlineSmall)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(venta.clienteNombre, style = MaterialTheme.typography.titleLarge)
                Text("${venta.cantidad}x ${venta.modelo}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Debe ahorita: ${formatMoney(venta.saldo)}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = CrimsonError,
                )
            }

            Text("Toca un monto rápido o escríbelo:", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(100.0, 200.0, 500.0).filter { it < venta.saldo }.forEach { quick ->
                    FilterChip(
                        selected = montoNum == quick,
                        onClick = { montoText = quick.toInt().toString(); errorMsg = null },
                        label = { Text(formatMoney(quick)) },
                        modifier = Modifier.height(44.dp),
                    )
                }
                FilterChip(
                    selected = montoNum == venta.saldo && venta.saldo > 0.0,
                    onClick = {
                        montoText = if (venta.saldo % 1.0 == 0.0) venta.saldo.toInt().toString() else venta.saldo.toString()
                        errorMsg = null
                    },
                    label = { Text("Liquidar todo (${formatMoney(venta.saldo)})") },
                    modifier = Modifier.height(44.dp),
                )
            }

            OutlinedTextField(
                value = montoText,
                onValueChange = { montoText = it; errorMsg = null },
                label = { Text("¿Cuánto abona hoy? ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("abono_monto_input"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )

            Text(
                text = if (montoNum > 0) {
                    "Nuevo saldo restante: ${formatMoney(nuevoSaldo)}"
                } else {
                    "Escribe o toca cuánto va a abonar"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (nuevoSaldo <= 0.0 && montoNum > 0) EmeraldSuccess else MaterialTheme.colorScheme.onSurface,
            )

            if (nuevoSaldo > 0.0) {
                Text(
                    text = "Siguiente cobro: ${formatFriendlyDate(proximaFecha)}",
                    style = MaterialTheme.typography.labelLarge,
                )
                QuickDateButtonsRow(
                    selectedIso = proximaFecha,
                    onSelectIso = { proximaFecha = it },
                    onOpenCalendar = { showDatePicker = true },
                )
            }

            if (errorMsg != null) {
                Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    if (montoNum <= 0.0) {
                        errorMsg = "Escribe un abono mayor a $0."
                        return@Button
                    }
                    onSaveAbono(montoNum, if (nuevoSaldo > 0.0) proximaFecha else "")
                },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_abono_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Text(
                    text = if (isBusy) "Guardando..." else "Guardar abono",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        IsoDatePickerDialog(
            onDismiss = { showDatePicker = false },
            onConfirmIso = {
                proximaFecha = it
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun QuickDateButtonsRow(
    selectedIso: String,
    onSelectIso: (String) -> Unit,
    onOpenCalendar: () -> Unit,
) {
    val today = remember { todayIsoDate() }
    val en7 = remember { addDaysIsoDate(today, 7) }
    val en15 = remember { addDaysIsoDate(today, 15) }
    val en30 = remember { addDaysIsoDate(today, 30) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedIso == en7,
            onClick = { onSelectIso(en7) },
            label = { Text("En 8 días") },
            modifier = Modifier.height(44.dp),
        )
        FilterChip(
            selected = selectedIso == en15,
            onClick = { onSelectIso(en15) },
            label = { Text("En 15 días") },
            modifier = Modifier.height(44.dp),
        )
        FilterChip(
            selected = selectedIso == en30,
            onClick = { onSelectIso(en30) },
            label = { Text("En 1 mes") },
            modifier = Modifier.height(44.dp),
        )
        OutlinedButton(
            onClick = onOpenCalendar,
            modifier = Modifier.height(44.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(" Elegir día")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IsoDatePickerDialog(
    onDismiss: () -> Unit,
    onConfirmIso: (String) -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        onConfirmIso(sdf.format(Date(millis)))
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Elegir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    ) {
        DatePicker(state = state)
    }
}
