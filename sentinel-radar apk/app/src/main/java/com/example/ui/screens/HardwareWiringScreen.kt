package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RadarViewModel
import com.example.ui.theme.RadarAmber
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarDarkBackground
import com.example.ui.theme.RadarEmerald
import com.example.ui.theme.RadarRed

@Composable
fun HardwareWiringScreen(
    viewModel: RadarViewModel,
    modifier: Modifier = Modifier
) {
    val fresnel by viewModel.fresnelParams.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RadarDarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0B132B))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricalServices,
                        contentDescription = "Hardware Wiring",
                        tint = RadarCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "FR-05 ACTUATION CIRCUIT & HARDWARE WIRING",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = "High-speed 2N2222 transistor driver circuit for the 5V Piezo Buzzer and Red LED indicator, triggered by GPIO 25 on the Receiver ESP32.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 15.sp
                )
            }
        }

        // Circuit Schematic Canvas Diagram
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF070D1E))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "CIRCUIT SCHEMATIC (GPIO 25 -> 2N2222 -> BUZZER & LED)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF020617))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Draw ESP32 block on left
                        drawRoundRect(
                            color = Color(0xFF1E293B),
                            topLeft = Offset(20f, 30f),
                            size = Size(140f, 140f),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                        drawRoundRect(
                            color = Color(0xFF0EA5E9),
                            topLeft = Offset(20f, 30f),
                            size = Size(140f, 140f),
                            cornerRadius = CornerRadius(8f, 8f),
                            style = Stroke(2f)
                        )

                        // Draw 2N2222 Transistor in center
                        val txX = w * 0.52f
                        val txY = h * 0.52f
                        drawCircle(
                            color = Color(0xFF334155),
                            radius = 28f,
                            center = Offset(txX, txY)
                        )
                        drawCircle(
                            color = RadarCyan,
                            radius = 28f,
                            center = Offset(txX, txY),
                            style = Stroke(2f)
                        )

                        // Draw 5V Rail on top right
                        drawRect(
                            color = RadarRed,
                            topLeft = Offset(w * 0.72f, 25f),
                            size = Size(w * 0.24f, 24f)
                        )

                        // Draw Buzzer & LED block
                        drawRoundRect(
                            color = Color(0xFF1E293B),
                            topLeft = Offset(w * 0.74f, 75f),
                            size = Size(w * 0.22f, 80f),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = RadarRed,
                            topLeft = Offset(w * 0.74f, 75f),
                            size = Size(w * 0.22f, 80f),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = Stroke(1.5f)
                        )

                        // Wire lines:
                        // 1. GPIO 25 out to resistor
                        drawLine(
                            color = RadarCyan,
                            start = Offset(160f, 70f),
                            end = Offset(txX - 70f, 70f),
                            strokeWidth = 3f
                        )
                        // 1k Resistor
                        drawRoundRect(
                            color = Color(0xFFF59E0B),
                            topLeft = Offset(txX - 70f, 60f),
                            size = Size(40f, 20f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        // Resistor to Base
                        drawLine(
                            color = RadarCyan,
                            start = Offset(txX - 30f, 70f),
                            end = Offset(txX - 25f, txY),
                            strokeWidth = 3f
                        )

                        // 2. Emitter to GND
                        drawLine(
                            color = Color(0xFF94A3B8),
                            start = Offset(txX, txY + 28f),
                            end = Offset(txX, h - 20f),
                            strokeWidth = 3f
                        )

                        // 3. Collector to Buzzer/LED cathode
                        val wirePath = Path().apply {
                            moveTo(txX + 22f, txY - 14f)
                            lineTo(w * 0.74f, txY - 14f)
                            lineTo(w * 0.74f, 135f)
                        }
                        drawPath(wirePath, color = RadarAmber, style = Stroke(3f))

                        // 4. 5V rail down to Buzzer (+)
                        drawLine(
                            color = RadarRed,
                            start = Offset(w * 0.85f, 49f),
                            end = Offset(w * 0.85f, 75f),
                            strokeWidth = 3f
                        )

                        // Labels via nativeCanvas
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 22f
                                typeface = android.graphics.Typeface.MONOSPACE
                            }
                            drawText("ESP32-RX", 35f, 70f, paint)
                            paint.textSize = 18f
                            paint.color = android.graphics.Color.CYAN
                            drawText("GPIO 25", 35f, 100f, paint)

                            paint.color = android.graphics.Color.WHITE
                            drawText("1kΩ", txX - 65f, 54f, paint)
                            drawText("2N2222", txX - 35f, txY + 45f, paint)
                            drawText("GND", txX - 18f, h - 6f, paint)

                            paint.color = android.graphics.Color.RED
                            drawText("+5V VIN", w * 0.76f, 42f, paint)

                            paint.color = android.graphics.Color.WHITE
                            drawText("PIEZO", w * 0.78f, 102f, paint)
                            drawText("+ LED", w * 0.78f, 125f, paint)
                        }
                    }
                }
            }
        }

        // 5 Step Wiring Checklist
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0B132B))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "STEP-BY-STEP WIRING CHECKLIST",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.2.sp
                )

                WiringStepItem(
                    stepNum = 1,
                    text = "Connect GPIO 25 on the RX ESP32 to one side of a 1 kΩ resistor."
                )
                WiringStepItem(
                    stepNum = 2,
                    text = "Connect the other side of the 1 kΩ resistor to the Base (B) pin of a 2N2222 NPN Transistor."
                )
                WiringStepItem(
                    stepNum = 3,
                    text = "Connect the Emitter (E) pin of the 2N2222 transistor directly to GND on the ESP32."
                )
                WiringStepItem(
                    stepNum = 4,
                    text = "Connect the Collector (C) pin to the Negative (-) lead of the 5V Piezo Buzzer and Cathode (-) of Red LED (via 220 Ω resistor)."
                )
                WiringStepItem(
                    stepNum = 5,
                    text = "Connect Positive (+) of Buzzer and Anode (+) of LED to the 5V (VIN) pin on the RX ESP32."
                )
            }
        }

        // Fresnel Zone RF Corridor Calculator
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0B132B))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "Fresnel Calculator",
                        tint = RadarCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "1ST FRESNEL ZONE & BOOTH GEOMETRY CALCULATOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.2.sp
                    )
                }

                Text(
                    text = "Computes the exact cylindrical RF corridor radius r₁ = √(λ·d₁·d₂/d) to ensure the test subject is inside the primary multipath scattering zone.",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    fontFamily = FontFamily.Monospace
                )

                // Calculation Result Callout Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF020617))
                        .border(1.dp, RadarCyan, RoundedCornerShape(6.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "1ST FRESNEL RADIUS (CORRIDOR)",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "±${String.format("%.1f", fresnel.firstFresnelRadiusCm)} cm",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = RadarCyan
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "WAVELENGTH (λ): 12.3 cm",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = "CHANNEL 6 (2.437 GHz)",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = RadarEmerald
                        )
                        Text(
                            text = "ELEVATION: ~1.0m (Waist)",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = RadarAmber
                        )
                    }
                }

                // Total Distance Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TX-to-RX Link Distance (d):",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = "${String.format("%.1f", fresnel.totalDistanceMeters)} meters",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = RadarCyan
                        )
                    }
                    Slider(
                        value = fresnel.totalDistanceMeters.toFloat(),
                        onValueChange = {
                            viewModel.updateFresnelParams(
                                totalDist = it.toDouble(),
                                targetDist = (it * 0.5).toDouble(),
                                freqGhz = 2.437,
                                heightM = 1.0
                            )
                        },
                        valueRange = 1.5f..6.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = RadarCyan,
                            activeTrackColor = RadarCyan,
                            inactiveTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("fresnel_distance_slider")
                    )
                }

                // Elevation Recommendation Note
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0F172A))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Guideline",
                        tint = RadarAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Exhibition Rule: Elevate both ESP32 modules to ~1.0m height on non-metallic stands. Avoid flat metallic tables to eliminate ground-plane cancellation.",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}

@Composable
private fun WiringStepItem(stepNum: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(RadarCyan),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$stepNum",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF020617)
            )
        }
        Text(
            text = text,
            fontSize = 11.sp,
            color = Color(0xFFCBD5E1),
            lineHeight = 16.sp
        )
    }
}
