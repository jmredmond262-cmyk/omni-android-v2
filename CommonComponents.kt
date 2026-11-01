package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import com.example.model.StoreType
import com.example.ui.theme.AmberDeal
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RosePromo
import com.example.ui.theme.SlateNavy

@Composable
fun RatingBadge(rating: Float, reviewsCount: Int, modifier: Modifier = Modifier) {
    Surface(
        color = AmberDeal.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = AmberDeal,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = String.format("%.1f", rating),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = SlateNavy
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "($reviewsCount)",
                fontSize = 11.sp,
                color = SlateNavy.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
fun DistancePill(distanceMeters: Int, walkingMinutes: Int, modifier: Modifier = Modifier) {
    Surface(
        color = EmeraldPrimary.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsWalk,
                contentDescription = null,
                tint = EmeraldDark,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (distanceMeters < 1000) "${distanceMeters}m ($walkingMinutes min)" else "${String.format("%.1f", distanceMeters / 1000f)}km",
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = EmeraldDark
            )
        }
    }
}

@Composable
fun VideoTourTag(duration: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color.Black.copy(alpha = 0.75f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(RosePromo, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Tour $duration",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun PulsingStatusDot(
    color: Color = Color(0xFF10B981),
    sizeDp: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier.size(sizeDp * 1.6f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp * scale)
                .background(color.copy(alpha = alpha * 0.35f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(sizeDp)
                .background(color.copy(alpha = alpha), CircleShape)
        )
    }
}

@Composable
fun OpenStatusBadge(
    isOpen: Boolean,
    is24Hours: Boolean = false,
    isOnDuty: Boolean = false,
    openOnHolidays: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isEffectivelyOpen = isOpen || is24Hours || isOnDuty

    Surface(
        color = if (isEffectivelyOpen) Color(0xFF064E3B).copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
        ) {
            if (isEffectivelyOpen) {
                PulsingStatusDot(color = Color(0xFF34D399), sizeDp = 7.dp)
                Spacer(modifier = Modifier.width(6.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(Color(0xFFEF4444), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = when {
                    isOnDuty -> "💊 De Turno (Guardia)"
                    is24Hours -> "🌙 24hs Abierto"
                    isEffectivelyOpen -> "Abierto ahora"
                    else -> "Cerrado"
                },
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            if (openOnHolidays && isEffectivelyOpen) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "• Feriados",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun StoreTypeChip(type: StoreType, modifier: Modifier = Modifier) {
    val bgColor = when (type) {
        StoreType.SUPERMERCADO -> Color(0xFF0284C7)
        StoreType.ALMACEN -> Color(0xFFD97706)
        StoreType.KIOSCO -> Color(0xFFEF4444)
        StoreType.PANADERIA -> Color(0xFFB45309)
        StoreType.TIENDA_NATURAL -> Color(0xFF059669)
        StoreType.FARMACIA -> Color(0xFF0D9488)
        StoreType.GASTRONOMIA -> Color(0xFF00E676)
        StoreType.SERVICIOS -> Color(0xFFFF9100)
    }

    Surface(
        color = bgColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = type.label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = bgColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

fun getStoreGradient(type: StoreType): Brush {
    return when (type) {
        StoreType.SUPERMERCADO -> Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1)))
        StoreType.ALMACEN -> Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
        StoreType.KIOSCO -> Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFB91C1C)))
        StoreType.PANADERIA -> Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFF78350F)))
        StoreType.TIENDA_NATURAL -> Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF047857)))
        StoreType.FARMACIA -> Brush.linearGradient(listOf(Color(0xFF0D9488), Color(0xFF047857)))
        StoreType.GASTRONOMIA -> Brush.linearGradient(listOf(Color(0xFF00E676), Color(0xFF059669)))
        StoreType.SERVICIOS -> Brush.linearGradient(listOf(Color(0xFFFF9100), Color(0xFFD97706)))
    }
}
