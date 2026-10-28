package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.data.model.CsiPoint
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarRed
import kotlin.math.max

@Composable
fun RadarOscilloscopeCanvas(
    data: List<CsiPoint>,
    fallThreshold: Double,
    occupancyThreshold: Double,
    plotRawAmplitude: Boolean,
    isEmergency: Boolean,
    modifier: Modifier = Modifier
) {
    val waveformColor = if (isEmergency) RadarRed else RadarCyan
    val glowColor = if (isEmergency) RadarRed.copy(alpha = 0.25f) else RadarCyan.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF070D1E))
            .border(
                width = 1.dp,
                color = if (isEmergency) RadarRed.copy(alpha = 0.8f) else Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Calculate max domain
            val maxSample = data.maxOfOrNull { it.varianceDb } ?: 35.0
            val maxRaw = if (plotRawAmplitude) (data.maxOfOrNull { it.rawAmplitude } ?: 25.0) else 0.0
            val yMax = max(50.0, max(fallThreshold * 1.35, max(maxSample * 1.15, maxRaw * 1.15))).toFloat()

            // 1. Draw Grid Lines
            val gridColor = Color(0xFF1E293B)
            val gridDash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

            // Horizontal grid lines (every 10 dB)
            val gridStepDb = 10f
            var currDb = 0f
            while (currDb <= yMax) {
                val y = height - (currDb / yMax) * height
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f,
                    pathEffect = gridDash
                )

                // Draw dB text label using nativeCanvas
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(120, 100, 116, 139)
                        textSize = 24f
                        typeface = android.graphics.Typeface.MONOSPACE
                    }
                    drawText("${currDb.toInt()} dB", 10f, y - 6f, paint)
                }
                currDb += gridStepDb
            }

            // Vertical time grid lines
            val numVLines = 5
            for (i in 1..numVLines) {
                val x = (width / (numVLines + 1)) * i
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f,
                    pathEffect = gridDash
                )
            }

            // 2. Draw Threshold Reference Lines
            // Fall Threshold (dashed red)
            val fallY = (height - (fallThreshold.toFloat() / yMax) * height).coerceIn(0f, height)
            drawLine(
                color = RadarRed,
                start = Offset(0f, fallY),
                end = Offset(width, fallY),
                strokeWidth = 2.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
            )
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(240, 239, 68, 68)
                    textSize = 26f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                }
                drawText("FALL THRESHOLD: ${fallThreshold.toInt()} dB", width - 360f, fallY - 10f, paint)
            }

            // Micro-Doppler Threshold (dashed cyan)
            val occY = (height - (occupancyThreshold.toFloat() / yMax) * height).coerceIn(0f, height)
            drawLine(
                color = Color(0xFF0891B2),
                start = Offset(0f, occY),
                end = Offset(width, occY),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(200, 8, 145, 178)
                    textSize = 24f
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                drawText("MICRO-DOPPLER: ${occupancyThreshold.toInt()} dB", width - 360f, occY - 8f, paint)
            }

            if (data.size < 2) return@Canvas

            val stepX = width / (data.size - 1).toFloat()

            // 3. Optional Raw CSI Amplitude Line (Slate 500)
            if (plotRawAmplitude) {
                val rawPath = Path()
                data.forEachIndexed { i, pt ->
                    val x = i * stepX
                    val y = (height - (pt.rawAmplitude.toFloat() / yMax) * height).coerceIn(0f, height)
                    if (i == 0) rawPath.moveTo(x, y) else rawPath.lineTo(x, y)
                }
                drawPath(
                    path = rawPath,
                    color = Color(0xFF64748B),
                    style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 4. Primary Filtered Variance Waveform (Cyan 400 / Red 500)
            val variancePath = Path()
            val fillPath = Path()

            data.forEachIndexed { i, pt ->
                val x = i * stepX
                val y = (height - (pt.varianceDb.toFloat() / yMax) * height).coerceIn(0f, height)
                if (i == 0) {
                    variancePath.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    variancePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(width, height)
            fillPath.close()

            // Draw area glow gradient
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw main waveform stroke
            drawPath(
                path = variancePath,
                color = waveformColor,
                style = Stroke(
                    width = if (isEmergency) 4f else 3.2f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Latest sample highlight dot
            val lastPt = data.last()
            val lastX = (data.size - 1) * stepX
            val lastY = (height - (lastPt.varianceDb.toFloat() / yMax) * height).coerceIn(0f, height)
            drawCircle(
                color = waveformColor,
                radius = 6f,
                center = Offset(lastX, lastY)
            )
            drawCircle(
                color = Color.White,
                radius = 2.5f,
                center = Offset(lastX, lastY)
            )
        }
    }
}
