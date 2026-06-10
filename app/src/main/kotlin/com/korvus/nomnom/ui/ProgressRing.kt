package com.korvus.nomnom.ui

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

/**
 * Большое кольцо прогресса дня. По центру — гигантская цифра ккал + подпись.
 * Кольцо — мягкий arc от 12 до 348 градусов с заострёнными концами (cap=Round).
 */
@Composable
fun KcalRing(
    kcal: Int,
    target: Int,
    size: Dp = 240.dp,
    stroke: Dp = 18.dp,
) {
    val pct = (kcal.toFloat() / target).coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = pct,
        animationSpec = tween(900),
        label = "kcal-ring"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            val sweepTotal = 320f
            val startAngle = 110f

            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            drawArc(
                color = activeColor,
                startAngle = startAngle,
                sweepAngle = sweepTotal * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
        // центральный текст
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "СЕГОДНЯ",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                kcal.toString(),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 56.sp,
            )
            Text(
                "ккал · из $target",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
fun MacroBar(value: Int, target: Int, color: Color, height: Dp = 6.dp) {
    val pct = if (target > 0) (value.toFloat() / target).coerceIn(0f, 1f) else 0f
    val anim by animateFloatAsState(pct, tween(600), label = "macro-bar")
    Canvas(
        modifier = Modifier
            .height(height)
            .fillMaxSize()
    ) {
        val h = size.height
        val w = size.width
        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2, h / 2)
        drawRoundRect(
            color = color.copy(alpha = 0.18f),
            size = Size(w, h),
            cornerRadius = cornerRadius,
        )
        drawRoundRect(
            color = color,
            size = Size(w * anim, h),
            cornerRadius = cornerRadius,
        )
    }
}
