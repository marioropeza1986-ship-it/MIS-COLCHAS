package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.ColchaCatalogOptions
import com.example.data.Producto
import com.example.data.ProductoConStock
import com.example.ui.components.LocationBadge
import com.example.ui.components.QuiltThumbnail
import com.example.ui.components.StatusPill
import com.example.ui.components.formatMoney
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CrimsonOnContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldSuccess

@Composable
fun ColchasScreen(
    productos: List<ProductoConStock>,
    onAgregarNueva: () -> Unit,
    onEditarProducto: (ProductoConStock) -> Unit,
    onReabastecer: (ProductoConStock) -> Unit,
    onVenderProducto: (ProductoConStock) -> Unit,
    onCambiarUbicacion: (Producto, String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("") }
    var productoParaUbicacion by remember { mutableStateOf<Producto?>(null) }

    val filtered = remember(productos, searchQuery, selectedLocation) {
        val q = searchQuery.trim().lowercase()
        productos.filter { item ->
            val p = item.producto
            val matchesQuery = q.isEmpty() ||
                p.modelo.lowercase().contains(q) ||
                p.tamano.lowercase().contains(q) ||
                p.color.lowercase().contains(q) ||
                p.sku.lowercase().contains(q)
            val matchesLoc = selectedLocation.isEmpty() || p.ubicacion == selectedLocation
            matchesQuery && matchesLoc
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mis Colchas",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Toca la foto o \"Editar\" para cambiar lugar o precio",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = onAgregarNueva,
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("add_quilt_button"),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Nueva", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_quilt_input"),
                placeholder = { Text("Buscar por nombre, tamaño o color...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedLocation.isEmpty(),
                    onClick = { selectedLocation = "" },
                    label = { Text("Todas (${productos.size})") },
                    modifier = Modifier.height(44.dp),
                )
                ColchaCatalogOptions.ubicaciones.forEach { loc ->
                    FilterChip(
                        selected = selectedLocation == loc,
                        onClick = { selectedLocation = if (selectedLocation == loc) "" else loc },
                        label = { Text(loc) },
                        modifier = Modifier.height(44.dp),
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp),
                        )
                        Text(
                            text = if (productos.isEmpty()) {
                                "Todavía no tienes colchas anotadas"
                            } else {
                                "No encontramos colchas con ese filtro"
                            },
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = if (productos.isEmpty()) {
                                "Presiona el botón de abajo para guardar tu primer modelo con su foto y precio."
                            } else {
                                "Prueba borrando la búsqueda o tocando \"Todas\"."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (productos.isEmpty()) {
                            Button(
                                onClick = onAgregarNueva,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Agregar mi primera colcha", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        } else {
            items(filtered, key = { it.producto.id }) { item ->
                val p = item.producto
                val disp = item.disponibles
                val (badgeLabel, badgeBg, badgeFg) = when {
                    disp <= 0L -> Triple("Agotada", CrimsonContainer, CrimsonOnContainer)
                    disp <= p.stockMinimo -> Triple("Quedan pocas", AmberContainer, AmberOnContainer)
                    else -> Triple("Disponible", EmeraldContainer, EmeraldOnContainer)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            QuiltThumbnail(
                                fotoBase64 = p.fotoBase64,
                                contentDescription = "Foto de ${p.modelo}",
                                size = 84.dp,
                                onClick = { onEditarProducto(item) },
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = p.modelo,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    text = buildString {
                                        append(p.tamano)
                                        if (p.color.isNotBlank()) append(" · ${p.color}")
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    StatusPill(
                                        text = badgeLabel,
                                        containerColor = badgeBg,
                                        contentColor = badgeFg,
                                    )
                                    Text(
                                        text = p.sku,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        // Location + Big Stock & Price numbers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LocationBadge(
                                ubicacion = p.ubicacion,
                                onClick = { productoParaUbicacion = p },
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Piezas",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "$disp",
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (disp <= 0L) CrimsonError else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Precio venta",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = formatMoney(p.precio),
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldSuccess,
                                    )
                                }
                            }
                        }

                        // Action buttons — large 48dp+ touch targets with clear labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { onEditarProducto(item) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Editar", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { onReabastecer(item) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Surtir", maxLines = 1)
                            }
                            Button(
                                onClick = { onVenderProducto(item) },
                                enabled = disp > 0L,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ),
                            ) {
                                Icon(Icons.Rounded.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Vender", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }

    // Quick location switcher dialog
    productoParaUbicacion?.let { prod ->
        AlertDialog(
            onDismissRequest = { productoParaUbicacion = null },
            title = { Text("¿Dónde está ${prod.modelo}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColchaCatalogOptions.ubicaciones.forEach { loc ->
                        val isCurrent = prod.ubicacion == loc
                        Button(
                            onClick = {
                                onCambiarUbicacion(prod, loc)
                                productoParaUbicacion = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = if (isCurrent) {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                        ) {
                            Text(loc, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { productoParaUbicacion = null }) {
                    Text("Cerrar")
                }
            },
        )
    }
}
