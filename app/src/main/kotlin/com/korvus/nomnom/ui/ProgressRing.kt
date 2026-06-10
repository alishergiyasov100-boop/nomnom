package com.korvus.nomnom.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Кольцо ккал на фиолетовом hero — track белый-полупрозрачный, fill белый чистый.
 * По центру — крупная цифра ккал на белом фоне (без подписи).
 */
@Composable
fun HeroKcalRing(
    kcal: Int,
    target: Int,
    size: Dp = 110.dp,
    stroke: Dp = 10.dp,
) {
    val pct = if (target > 0) (kcal.toFloat() / target).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(targetValue = pct, animationSpec = tween(900), label = "hero-ring")

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val arcSize = Size(this.size.width - s, this.size.height - s)
            val topLeft = Offset(s / 2, s / 2)
            drawArc(
                color = Color.White.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = s, cap = StrokeCap.Round),
            )
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = s, cap = StrokeCap.Round),
            )
        }
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                kcal.toString(),
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 32.sp,
            )
            Text(
                "ккал",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun MacroBar(
    value: Int,
    target: Int,
    activeColor: Color,
    trackColor: Color,
    height: Dp = 6.dp,
) {
    val pct = if (target > 0) (value.toFloat() / target).coerceIn(0f, 1f) else 0f
    val anim by animateFloatAsState(pct, tween(700), label = "macro-bar")
    Canvas(modifier = Modifier.height(height).fillMaxSize()) {
        val h = size.height
        val w = size.width
        val cr = CornerRadius(h / 2, h / 2)
        drawRoundRect(color = trackColor, size = Size(w, h), cornerRadius = cr)
        drawRoundRect(color = activeColor, size = Size((w * anim).coerceAtLeast(h), h), cornerRadius = cr)
    }
}

@Composable
fun MacroBarOnHero(value: Int, target: Int, height: Dp = 6.dp) {
    MacroBar(
        value = value,
        target = target,
        activeColor = Color.White,
        trackColor = Color.White.copy(alpha = 0.22f),
        height = height,
    )
}
