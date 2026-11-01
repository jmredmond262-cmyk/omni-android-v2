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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun OmniSplashScreen() {
    val logoProgress = remember { Animatable(0f) }
    val nameProgress = remember { Animatable(0f) }
    val pulse = remember { Animatable(0.45f) }
    LaunchedEffect(Unit) {
        logoProgress.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))
        delay(180)
        nameProgress.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
        while (true) {
            pulse.animateTo(0.9f, tween(1100))
            pulse.animateTo(0.45f, tween(1100))
        }
    }
    val blue = Color(0xFF087BFF)
    val cyan = Color(0xFF20D9FF)
    Column(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF020612), Color(0xFF071A3A), Color(0xFF020612)))
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(Modifier.size(218.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension * 0.34f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(cyan.copy(alpha = 0.25f * pulse.value), blue.copy(alpha = 0.12f), Color.Transparent),
                    center = c, radius = r * 2.25f
                ),
                radius = r * 2.25f, center = c
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(cyan, blue, Color(0xFF1746D8), cyan)),
                startAngle = -90f, sweepAngle = 360f * logoProgress.value,
                useCenter = false,
                topLeft = Offset(c.x-r, c.y-r), size = Size(r*2, r*2),
                style = Stroke(width = r*0.38f, cap = StrokeCap.Round)
            )
            if (logoProgress.value > 0.55f) {
                val p = ((logoProgress.value - 0.55f) / 0.45f).coerceIn(0f, 1f)
                drawLine(
                    brush = Brush.linearGradient(listOf(cyan, blue)),
                    start = Offset(c.x-r*0.62f, c.y+r*0.42f),
                    end = Offset(c.x+r*0.70f, c.y-r*0.40f),
                    strokeWidth = r*0.24f*p,
                    cap = StrokeCap.Round
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            "OMNI",
            modifier = Modifier.alpha(nameProgress.value).scale(0.82f + nameProgress.value*0.18f),
            color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 7.sp
        )
        Spacer(Modifier.height(18.dp))
        Box(Modifier.width(90.dp).height(3.dp).alpha(nameProgress.value)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, cyan, blue, Color.Transparent)), CircleShape))
    }
}
