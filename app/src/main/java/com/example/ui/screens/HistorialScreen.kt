package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.EntradaStock
import com.example.data.Venta
import com.example.ui.DashboardData
import com.example.ui.components.StatusPill
import com.example.ui.components.formatFriendlyDate
import com.example.ui.components.formatMoney
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.AppVisualStyle
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldSuccess

private enum class HistorialTab(val label: String) {
    VENTAS("Ventas"),
    ENTRADAS("Entradas"),
    AJUSTES("Resumen y Ajustes"),
}

@Composable
fun HistorialScreen(
    data: DashboardData,
    userEmail: String?,
    largeTextEnabled: Boolean,
    themeMode: String,
    visualStyle: AppVisualStyle,
    onToggleLargeText: (Boolean) -> Unit,
    onChangeThemeMode: (String) -> Unit,
    onChangeVisualStyle: (AppVisualStyle) -> Unit,
    onDeleteVenta: (Venta) -> Unit,
    onSignOut: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(HistorialTab.VENTAS) }
    var ventaAEliminar by remember { mutableStateOf<Venta?>(null) }
    var confirmSignOut by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                text = "Historial y Ajustes",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                HistorialTab.entries.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = HistorialTab.entries.size),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text(tab.label, maxLines = 1)
                    }
                }
            }
        }

        when (selectedTab) {
            HistorialTab.VENTAS -> {
                if (data.ventas.isEmpty()) {
                    item {
                        EmptyHistoryCard(
                            icon = Icons.Rounded.ReceiptLong,
                            title = "Aún no hay ventas registradas",
                            subtitle = "Cada venta de contado o en abonos aparecerá aquí.",
                        )
                    }
                } else {
                    items(data.ventas, key = { "v_${it.id}" }) { venta ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = venta.clienteNombre,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = "${venta.cantidad}x ${venta.modelo} · ${formatFriendlyDate(venta.fecha)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = formatMoney(venta.importeTotal),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = EmeraldSuccess,
                                        )
                                        if (venta.saldo > 0.0) {
                                            StatusPill(
                                                text = "Resta ${formatMoney(venta.saldo)}",
                                                containerColor = AmberContainer,
                                                contentColor = AmberOnContainer,
                                            )
                                        } else {
                                            StatusPill(
                                                text = "Pagado",
                                                containerColor = EmeraldContainer,
                                                contentColor = EmeraldOnContainer,
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { ventaAEliminar = venta },
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "Eliminar venta",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HistorialTab.ENTRADAS -> {
                if (data.entradas.isEmpty()) {
                    item {
                        EmptyHistoryCard(
                            icon = Icons.Rounded.Inventory,
                            title = "Aún no hay entradas registradas",
                            subtitle = "Cuando surtas más piezas de un modelo aparecerán aquí.",
                        )
                    }
                } else {
                    items(data.entradas, key = { "e_${it.id}" }) { entrada ->
                        EntradaCard(entrada)
                    }
                }
            }

            HistorialTab.AJUSTES -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "Resumen por tamaño",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            if (data.resumenPorTamano.isEmpty()) {
                                Text(
                                    text = "Agrega colchas para ver cuántas piezas tienes de cada tamaño.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                data.resumenPorTamano.forEachIndexed { idx, res ->
                                    if (idx > 0) HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column {
                                            Text(res.tamano, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "${res.modelos} modelo(s)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "${res.piezasDisponibles} piezas",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            )
                                            Text(
                                                "Costo: ${formatMoney(res.valorCosto)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Accessibility & Comfort Settings for Mom
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Text(
                                text = "Comodidad de lectura",
                                style = MaterialTheme.typography.titleLarge,
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(Icons.Rounded.FormatSize, contentDescription = null, modifier = Modifier.size(28.dp))
                                    Column {
                                        Text("Letra extra grande", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Aumenta el tamaño de textos y números",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Switch(
                                    checked = largeTextEnabled,
                                    onCheckedChange = onToggleLargeText,
                                    modifier = Modifier.testTag("large_text_switch"),
                                )
                            }

                            HorizontalDivider()

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(Icons.Rounded.DarkMode, contentDescription = null)
                                Text("Modo de pantalla", style = MaterialTheme.typography.titleMedium)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf("light" to "Día (Claro)", "dark" to "Noche (Oscuro)").forEach { (mode, label) ->
                                    val active = themeMode == mode
                                    Button(
                                        onClick = { onChangeThemeMode(mode) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
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

                            HorizontalDivider()

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(Icons.Rounded.Palette, contentDescription = null)
                                Text("Color y estilo", style = MaterialTheme.typography.titleMedium)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                AppVisualStyle.entries.forEach { style ->
                                    val active = visualStyle == style
                                    Button(
                                        onClick = { onChangeVisualStyle(style) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
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
                                        Text(style.label, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick practical guide
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Rounded.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Guía rápida", style = MaterialTheme.typography.titleLarge)
                            }
                            Text("1. En Colchas anotas tus modelos, fotos y dónde están guardados (Tienda, Bodega o Con mi hermana).", style = MaterialTheme.typography.bodyMedium)
                            Text("2. En Inicio tocas \"Vender una colcha\" y eliges si pagó completo o en abonos.", style = MaterialTheme.typography.bodyMedium)
                            Text("3. En Cobros ves cuánto debe cada clienta, registras sus abonos y puedes mandar su nota por WhatsApp.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Account & Sign Out
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(Icons.Rounded.AccountCircle, contentDescription = null, modifier = Modifier.size(30.dp))
                                Column {
                                    Text("Cuenta conectada", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = userEmail ?: "Sesión de Google activa",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Button(
                                onClick = { confirmSignOut = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("sign_out_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Cerrar sesión")
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }

    ventaAEliminar?.let { v ->
        AlertDialog(
            onDismissRequest = { ventaAEliminar = null },
            title = { Text("¿Borrar esta venta?") },
            text = { Text("Se borrará la venta de ${v.clienteNombre} (${v.cantidad}x ${v.modelo}). Las piezas volverán al inventario.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteVenta(v)
                        ventaAEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Sí, borrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { ventaAEliminar = null }) { Text("Cancelar") }
            },
        )
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tus datos quedan guardados en tu cuenta de Google. Cuando vuelvas a entrar aparecerán igual.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmSignOut = false
                        onSignOut()
                    },
                ) {
                    Text("Cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("Quedarme") }
            },
        )
    }
}

@Composable
private fun EntradaCard(entrada: EntradaStock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entrada.modelo, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = buildString {
                        append(formatFriendlyDate(entrada.fecha))
                        if (entrada.proveedor.isNotBlank()) append(" · ${entrada.proveedor}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusPill(
                    text = "+${entrada.cantidad} pzas",
                    containerColor = EmeraldContainer,
                    contentColor = EmeraldOnContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${formatMoney(entrada.costo)} c/u",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
