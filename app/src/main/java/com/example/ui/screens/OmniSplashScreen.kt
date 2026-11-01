package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun OmniSplashScreen() {
    val reveal = remember { Animatable(0f) }
    val wordReveal = remember { Animatable(0f) }
    val glow = remember { Animatable(0.35f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(1350, easing = FastOutSlowInEasing))
        delay(180)
        wordReveal.animateTo(1f, tween(850, easing = FastOutSlowInEasing))
        while (true) {
            glow.animateTo(0.85f, tween(950))
            glow.animateTo(0.35f, tween(950))
        }
    }
    val blue = Color(0xFF087BFF)
    val cyan = Color(0xFF19D9FF)
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF020716), Color(0xFF071638), Color(0xFF020611)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxWidth().height(330.dp).align(Alignment.Center)) {
            val center = Offset(size.width / 2f, size.height * 0.42f)
            val radius = size.minDimension * 0.235f
            // Halo luminoso; el isotipo es un anillo azul segmentado, sin órbitas externas.
            drawCircle(
                Brush.radialGradient(listOf(cyan.copy(alpha = 0.16f * glow.value), blue.copy(alpha = 0.10f), Color.Transparent), center, radius * 2.0f),
                radius = radius * 2.0f,
                center = center
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(cyan, blue, Color(0xFF1746D8), cyan)),
                startAngle = -90f,
                sweepAngle = 360f * reveal.value,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = radius * 0.34f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            // Corte diagonal que da al círculo el gesto dinámico del isotipo Omni.
            if (reveal.value > 0.72f) {
                drawLine(
                    Brush.horizontalGradient(listOf(Color.Transparent, cyan, blue)),
                    Offset(center.x - radius * 0.72f, center.y + radius * 0.48f),
                    Offset(center.x + radius * 0.85f, center.y - radius * 0.42f),
                    strokeWidth = radius * 0.22f * ((reveal.value - 0.72f) / 0.28f)
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = 115.dp)
        ) {
            Text(
                "Omni",
                modifier = Modifier.alpha(wordReveal.value).scale(0.88f + wordReveal.value * 0.12f),
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.5).sp,
                color = Color.White
            )
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.width(74.dp).height(3.dp).alpha(wordReveal.value)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, cyan, blue, Color.Transparent)), CircleShape)
            )
        }
    }
}
