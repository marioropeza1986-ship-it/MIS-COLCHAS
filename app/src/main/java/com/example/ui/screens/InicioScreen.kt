package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ProductoConStock
import com.example.data.Venta
import com.example.ui.DashboardData
import com.example.ui.components.KpiCard
import com.example.ui.components.QuiltThumbnail
import com.example.ui.components.StatusPill
import com.example.ui.components.formatMoney
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AppVisualStyle
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonOnContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LocalVisualStyle

@Composable
fun InicioScreen(
    data: DashboardData,
    userName: String?,
    visualStyle: AppVisualStyle,
    onSelectVisualStyle: (AppVisualStyle) -> Unit,
    onVenderClick: () -> Unit,
    onIrACobrosClick: () -> Unit,
    onAgregarColchaClick: () -> Unit,
    onEditarProducto: (ProductoConStock) -> Unit,
    onVenderProducto: (ProductoConStock) -> Unit,
    onReabastecerProducto: (ProductoConStock) -> Unit,
    onAbonarVenta: (Venta) -> Unit,
) {
    val saludoNombre = userName?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() }
    val isApple = LocalVisualStyle.current == AppVisualStyle.APPLE
    val actionButtonShape = if (isApple) RoundedCornerShape(999.dp) else RoundedCornerShape(18.dp)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1-tap visual style preview bar (Terracota / Azul / Estilo Apple)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(if (isApple) 20.dp else 18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = if (isApple) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = if (isApple) 0.dp else 1.dp),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Prueba el estilo que más te guste:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        AppVisualStyle.entries.forEachIndexed { index, style ->
                            SegmentedButton(
                                selected = visualStyle == style,
                                onClick = { onSelectVisualStyle(style) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = AppVisualStyle.entries.size,
                                ),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("style_chip_${style.key}"),
                            ) {
                                Text(style.label, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Simple greeting & two essential numbers
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (saludoNombre != null) "Hola, $saludoNombre" else "Inventario de Colchas",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    KpiCard(
                        label = "Colchas en total",
                        value = "${data.totalPiezasDisponibles}",
                        accentColor = MaterialTheme.colorScheme.primary,
                        icon = Icons.Rounded.Bed,
                        modifier = Modifier.weight(1f),
                    )
                    KpiCard(
                        label = "Por cobrar",
                        value = formatMoney(data.porCobrarTotal),
                        accentColor = AmberWarning,
                        icon = Icons.Rounded.Payments,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Big, easy-to-tap primary buttons right at the top
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onAgregarColchaClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .testTag("home_add_quilt_button"),
                        shape = actionButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Agregar",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                        )
                    }

                    Button(
                        onClick = onVenderClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .testTag("home_sell_button"),
                        shape = actionButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ),
                    ) {
                        Icon(Icons.Rounded.PointOfSale, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Vender",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                        )
                    }
                }

                if (data.cobrosUrgentes.isNotEmpty()) {
                    val primera = data.cobrosUrgentes.first()
                    Surface(
                        onClick = {
                            if (data.cobrosUrgentes.size == 1) onAbonarVenta(primera) else onIrACobrosClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_urgent_collect_banner"),
                        shape = RoundedCornerShape(if (isApple) 20.dp else 18.dp),
                        color = AmberContainer,
                        contentColor = AmberOnContainer,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                                Column {
                                    Text(
                                        text = if (data.cobrosUrgentes.size == 1) {
                                            "Cobro pendiente hoy: ${primera.clienteNombre}"
                                        } else {
                                            "Tienes ${data.cobrosUrgentes.size} cobros pendientes hoy"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = "Toca aquí para registrar el abono",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                            Icon(Icons.Rounded.AttachMoney, contentDescription = null, modifier = Modifier.size(26.dp))
                        }
                    }
                }
            }
        }

        // Section Header: Simple Inventory List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Lista de colchas (${data.productosConStock.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (data.ventasPendientes.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onIrACobrosClick,
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("home_collect_button"),
                        shape = actionButtonShape,
                    ) {
                        Text("Ver cobros (${data.ventasPendientes.size})")
                    }
                }
            }
        }

        if (data.productosConStock.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(if (isApple) 24.dp else 22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = if (isApple) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isApple) 0.dp else 1.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(54.dp),
                        )
                        Text(
                            text = "Tu lista de colchas está vacía",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "Toca el botón grande para guardar el nombre, cantidad y precio de tu primera colcha.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onAgregarColchaClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = actionButtonShape,
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Agregar primera colcha",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
        } else {
            items(data.productosConStock, key = { "home_prod_${it.producto.id}" }) { item ->
                SimpleInventoryListItem(
                    item = item,
                    isApple = isApple,
                    onEdit = { onEditarProducto(item) },
                    onRestock = { onReabastecerProducto(item) },
                    onSell = { onVenderProducto(item) },
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun SimpleInventoryListItem(
    item: ProductoConStock,
    isApple: Boolean,
    onEdit: () -> Unit,
    onRestock: () -> Unit,
    onSell: () -> Unit,
) {
    val p = item.producto
    val disp = item.disponibles
    val (stockBg, stockFg) = when {
        disp <= 0L -> CrimsonContainer to CrimsonOnContainer
        disp <= p.stockMinimo -> AmberContainer to AmberOnContainer
        else -> EmeraldContainer to EmeraldOnContainer
    }
    val itemButtonShape = if (isApple) RoundedCornerShape(999.dp) else RoundedCornerShape(14.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("home_quilt_card_${p.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isApple) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isApple) 0.dp else 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuiltThumbnail(
                    fotoBase64 = p.fotoBase64,
                    contentDescription = "Foto de ${p.modelo}",
                    size = 72.dp,
                    onClick = onEdit,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = p.modelo,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${p.tamano} · ${p.ubicacion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusPill(
                            text = if (disp == 1L) "1 pieza" else "$disp piezas",
                            containerColor = stockBg,
                            contentColor = stockFg,
                        )
                        Text(
                            text = formatMoney(p.precio),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldSuccess,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = itemButtonShape,
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Editar", maxLines = 1)
                }

                OutlinedButton(
                    onClick = onRestock,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = itemButtonShape,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Surtir", maxLines = 1)
                }

                Button(
                    onClick = onSell,
                    enabled = disp > 0L,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = itemButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (disp > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Icon(Icons.Rounded.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (disp > 0L) "Vender" else "Agotada", maxLines = 1)
                }
            }
        }
    }
}
