package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.Venta
import com.example.ui.theme.AppVisualStyle
import com.example.ui.theme.LocalVisualStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val mxnFormatter: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("es", "MX")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
}

fun formatMoney(amount: Double): String = mxnFormatter.format(amount)

fun todayIsoDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(Date())
}

fun addDaysIsoDate(baseIso: String, days: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = sdf.parse(baseIso) ?: Date()
        val cal = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.DAY_OF_YEAR, days)
        }
        sdf.format(cal.time)
    } catch (_: Exception) {
        todayIsoDate()
    }
}

fun formatFriendlyDate(isoDate: String): String {
    if (isoDate.isBlank()) return "Sin fecha"
    val today = todayIsoDate()
    if (isoDate == today) return "Hoy"
    if (isoDate == addDaysIsoDate(today, 1)) return "Mañana"
    return try {
        val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val outFmt = SimpleDateFormat("d MMM yyyy", Locale("es", "MX"))
        val date = inFmt.parse(isoDate)
        if (date != null) outFmt.format(date) else isoDate
    } catch (_: Exception) {
        isoDate
    }
}

fun isOverdueDate(isoDate: String): Boolean {
    if (isoDate.isBlank()) return false
    return isoDate < todayIsoDate()
}

suspend fun compressImageUriToBase64(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val original = stream.use { BitmapFactory.decodeStream(it) } ?: return@withContext null

            val maxDim = 260
            val width = original.width
            val height = original.height
            val scale = if (width >= height && width > maxDim) {
                maxDim.toFloat() / width.toFloat()
            } else if (height > width && height > maxDim) {
                maxDim.toFloat() / height.toFloat()
            } else {
                1f
            }

            val targetW = (width * scale).toInt().coerceAtLeast(1)
            val targetH = (height * scale).toInt().coerceAtLeast(1)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(original, targetW, targetH, true)
            } else {
                original
            }

            var quality = 68
            var encoded = ""
            while (quality >= 30) {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                if (encoded.length <= 65000) break
                quality -= 12
            }

            if (scaled != original) scaled.recycle()
            original.recycle()

            encoded.takeIf { it.isNotBlank() && it.length <= 75000 }
        } catch (_: Exception) {
            null
        }
    }

fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? {
    if (base64.isNullOrBlank()) return null
    return try {
        val clean = base64.substringAfter("base64,", base64)
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

fun shareEstadoDeCuenta(context: Context, venta: Venta) {
    val msg = buildString {
        appendLine("Hola ${venta.clienteNombre}, le comparto su nota de colcha:")
        appendLine("• Colcha: ${venta.cantidad}x ${venta.modelo}")
        appendLine("• Total: ${formatMoney(venta.importeTotal)}")
        appendLine("• Abonado: ${formatMoney(venta.pagado)}")
        if (venta.saldo > 0.0) {
            appendLine("• Resta por pagar: ${formatMoney(venta.saldo)}")
            if (venta.proximoCobro.isNotBlank()) {
                appendLine("• Próximo abono: ${formatFriendlyDate(venta.proximoCobro)}")
            }
        } else {
            appendLine("• Estado: ¡Liquidada! Muchas gracias por su compra.")
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, msg)
    }
    context.startActivity(Intent.createChooser(intent, "Enviar nota por WhatsApp o mensaje"))
}

@Composable
fun QuiltThumbnail(
    fotoBase64: String?,
    contentDescription: String,
    size: Dp = 76.dp,
    onClick: (() -> Unit)? = null,
) {
    val bitmap = remember(fotoBase64) { decodeBase64ToImageBitmap(fotoBase64) }
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = if (onClick != null) Icons.Rounded.AddPhotoAlternate else Icons.Rounded.Bed,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.45f),
            )
        }
    }
}

@Composable
fun KpiCard(
    label: String,
    value: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val isApple = LocalVisualStyle.current == AppVisualStyle.APPLE
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(if (isApple) 22.dp else 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isApple) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isApple) 0.dp else 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.14f),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(20.dp),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun BigActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = contentColor.copy(alpha = 0.16f),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(28.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.9f),
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        modifier = if (onClick != null) {
            Modifier
                .minimumInteractiveComponentSize()
                .clickable(onClick = onClick)
        } else {
            Modifier
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        }
    }
}

@Composable
fun LocationBadge(
    ubicacion: String,
    onClick: () -> Unit,
) {
    StatusPill(
        text = ubicacion,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        icon = Icons.Rounded.LocationOn,
        onClick = onClick,
    )
}
