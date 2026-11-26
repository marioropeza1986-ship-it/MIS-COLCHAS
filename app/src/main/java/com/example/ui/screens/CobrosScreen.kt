package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.Venta
import com.example.ui.components.StatusPill
import com.example.ui.components.formatFriendlyDate
import com.example.ui.components.formatMoney
import com.example.ui.components.isOverdueDate
import com.example.ui.components.shareEstadoDeCuenta
import com.example.ui.components.todayIsoDate
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CrimsonOnContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldSuccess
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class CobrosTab(val label: String) {
    PENDIENTES("Por cobrar"),
    CALENDARIO("Calendario"),
    LIQUIDADAS("Liquidadas"),
}

@Composable
fun CobrosScreen(
    ventas: List<Venta>,
    onAbonarVenta: (Venta) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(CobrosTab.PENDIENTES) }
    var searchQuery by remember { mutableStateOf("") }

    val calInit = remember { Calendar.getInstance() }
    var calYear by remember { mutableIntStateOf(calInit.get(Calendar.YEAR)) }
    var calMonth by remember { mutableIntStateOf(calInit.get(Calendar.MONTH)) }
    var selectedCalendarDate by remember { mutableStateOf(todayIsoDate()) }

    val filtered = remember(ventas, selectedTab, searchQuery) {
        val q = searchQuery.trim().lowercase()
        val byQuery = ventas.filter { v ->
            q.isEmpty() ||
                v.clienteNombre.lowercase().contains(q) ||
                v.modelo.lowercase().contains(q)
        }
        when (selectedTab) {
            CobrosTab.PENDIENTES -> byQuery.filter { it.saldo > 0.0 }
                .sortedWith(compareBy<Venta> { it.proximoCobro.ifBlank { "9999-12-31" } }.thenByDescending { it.saldo })
            CobrosTab.LIQUIDADAS -> byQuery.filter { it.saldo <= 0.0 }
                .sortedByDescending { it.fecha }
            CobrosTab.CALENDARIO -> byQuery.filter { it.saldo > 0.0 }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Libreta de Cobros",
                    style = MaterialTheme.typography.headlineMedium,
                )
                val totalPorCobrar = ventas.sumOf { it.saldo.coerceAtLeast(0.0) }
                Text(
                    text = "Total por cobrar: ${formatMoney(totalPorCobrar)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = AmberWarning,
                )
            }
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                CobrosTab.entries.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = CobrosTab.entries.size),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text(tab.label, maxLines = 1)
                    }
                }
            }
        }

        if (selectedTab != CobrosTab.CALENDARIO) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_client_input"),
                    placeholder = { Text("Buscar nombre de clienta...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
            }

            if (filtered.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(44.dp),
                            )
                            Text(
                                text = if (selectedTab == CobrosTab.PENDIENTES) {
                                    "¡Sin deudas pendientes!"
                                } else {
                                    "Aún no hay cuentas liquidadas"
                                },
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = "Cuando registres una venta en pagos aparecerá aquí con sus abonos y próximo cobro.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { venta ->
                    ClientaVentaCard(venta = venta, onAbonar = { onAbonarVenta(venta) })
                }
            }
        } else {
            // Calendario mensual visual
            item {
                CalendarioCobrosCard(
                    year = calYear,
                    month = calMonth,
                    selectedDate = selectedCalendarDate,
                    ventasPendientes = filtered,
                    onPrevMonth = {
                        val c = Calendar.getInstance().apply {
                            set(Calendar.YEAR, calYear)
                            set(Calendar.MONTH, calMonth)
                            add(Calendar.MONTH, -1)
                        }
                        calYear = c.get(Calendar.YEAR)
                        calMonth = c.get(Calendar.MONTH)
                    },
                    onNextMonth = {
                        val c = Calendar.getInstance().apply {
                            set(Calendar.YEAR, calYear)
                            set(Calendar.MONTH, calMonth)
                            add(Calendar.MONTH, 1)
                        }
                        calYear = c.get(Calendar.YEAR)
                        calMonth = c.get(Calendar.MONTH)
                    },
                    onSelectDate = { selectedCalendarDate = it },
                )
            }

            val ventasDelDia = filtered.filter { it.proximoCobro == selectedCalendarDate }
            item {
                Text(
                    text = "Cobros para ${formatFriendlyDate(selectedCalendarDate)} (${ventasDelDia.size})",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            if (ventasDelDia.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Text(
                            text = "No hay clientas programadas para este día. Toca un día marcado en amarillo para ver sus cobros.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp),
                        )
                    }
                }
            } else {
                items(ventasDelDia, key = { "cal_${it.id}" }) { venta ->
                    ClientaVentaCard(venta = venta, onAbonar = { onAbonarVenta(venta) })
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun ClientaVentaCard(
    venta: Venta,
    onAbonar: () -> Unit,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val pendiente = venta.saldo > 0.0
    val atrasado = pendiente && isOverdueDate(venta.proximoCobro)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = venta.clienteNombre,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "${venta.cantidad}x ${venta.modelo} · ${formatFriendlyDate(venta.fecha)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (venta.clienteTelefono.isNotBlank() || venta.nota.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            if (venta.clienteTelefono.isNotBlank()) {
                                Icon(
                                    Icons.Rounded.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                            }
                            Text(
                                text = listOf(venta.clienteTelefono, venta.nota)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (pendiente) {
                    StatusPill(
                        text = if (atrasado) "Atrasado" else formatFriendlyDate(venta.proximoCobro),
                        containerColor = if (atrasado) CrimsonContainer else AmberContainer,
                        contentColor = if (atrasado) CrimsonOnContainer else AmberOnContainer,
                    )
                } else {
                    StatusPill(
                        text = "Liquidado",
                        containerColor = EmeraldContainer,
                        contentColor = EmeraldOnContainer,
                    )
                }
            }

            // Clear 3-number money breakdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMoney(venta.importeTotal), style = MaterialTheme.typography.titleMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Abonado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatMoney(venta.pagado),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldSuccess,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Resta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatMoney(venta.saldo),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (pendiente) CrimsonError else EmeraldSuccess,
                    )
                }
            }

            // Expandable payment history
            if (venta.historialPagos.isNotEmpty()) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.height(44.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Ver abonos (${venta.historialPagos.size})")
                }
                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        HorizontalDivider()
                        venta.historialPagos.forEach { pago ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${pago.concepto} · ${formatFriendlyDate(pago.fecha)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = formatMoney(pago.monto),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldSuccess,
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { shareEstadoDeCuenta(context, venta) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Enviar nota", maxLines = 1)
                }
                if (pendiente) {
                    Button(
                        onClick = onAbonar,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("abonar_button_${venta.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ),
                    ) {
                        Icon(Icons.Rounded.AttachMoney, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Abonar", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarioCobrosCard(
    year: Int,
    month: Int,
    selectedDate: String,
    ventasPendientes: List<Venta>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (String) -> Unit,
) {
    val monthTitle = remember(year, month) {
        val cal = Calendar.getInstance().apply { set(year, month, 1) }
        SimpleDateFormat("MMMM yyyy", Locale("es", "MX")).format(cal.time)
            .replaceFirstChar { it.uppercase() }
    }
    val firstDayOfWeek = remember(year, month) {
        val cal = Calendar.getInstance().apply { set(year, month, 1) }
        cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Domingo
    }
    val daysInMonth = remember(year, month) {
        val cal = Calendar.getInstance().apply { set(year, month, 1) }
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val cobrosPorDia = remember(ventasPendientes) {
        ventasPendientes.groupBy { it.proximoCobro }
    }
    val today = remember { todayIsoDate() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevMonth, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Mes anterior")
                }
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleLarge,
                )
                IconButton(onClick = onNextMonth, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Mes siguiente")
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb").forEach { d ->
                    Text(
                        text = d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (c in 0 until 7) {
                        val cellIndex = r * 7 + c
                        val dayNum = cellIndex - firstDayOfWeek + 1
                        if (dayNum < 1 || dayNum > daysInMonth) {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val dateIso = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayNum)
                            val count = cobrosPorDia[dateIso]?.size ?: 0
                            val isSelected = dateIso == selectedDate
                            val isToday = dateIso == today
                            val bg = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                count > 0 -> AmberContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            }
                            val fg = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                count > 0 -> AmberOnContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bg)
                                    .then(
                                        if (isToday && !isSelected) {
                                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { onSelectDate(dateIso) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$dayNum",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = fg,
                                    )
                                    if (count > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else AmberWarning)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
