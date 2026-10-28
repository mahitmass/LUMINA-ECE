package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RadarAmber
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarRed

@Composable
fun InteractivePerturbationCard(
    onSetEmpty: () -> Unit,
    onSetBreathing: () -> Unit,
    onTriggerBookDrop: () -> Unit,
    onTriggerFall: () -> Unit,
    onSliderDisturbance: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var manualSliderVal by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0B132B))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(16.dp)
            .testTag("interactive_perturbation_card"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Sensors,
                contentDescription = "RF Channel Excitation",
                tint = RadarAmber,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "RF MULTIPATH CHANNEL EXCITATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.2.sp
            )
        }

        Text(
            text = "Inject calibrated RF multipath scattering scenarios directly into the 100Hz DSP pipeline:",
            fontSize = 11.sp,
            color = Color(0xFFCBD5E1),
            fontFamily = FontFamily.Monospace
        )

        // 4 Action Preset Buttons in a 2x2 grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSetEmpty,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color(0xFFCBD5E1)
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f).testTag("preset_empty_button")
                ) {
                    Text(
                        text = "1. Empty Baseline\n(0.2 - 1.2 dB)",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 13.sp
                    )
                }

                Button(
                    onClick = onSetBreathing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x3322D3EE),
                        contentColor = RadarCyan
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f).testTag("preset_breathing_button")
                ) {
                    Text(
                        text = "2. Micro-Doppler\n(Breathing 3 dB)",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 13.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTriggerBookDrop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x33FBBF24),
                        contentColor = RadarAmber
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f).testTag("preset_book_drop_button")
                ) {
                    Text(
                        text = "3. Dropped Book\n(Impulse 9.5 dB)",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 13.sp
                    )
                }

                Button(
                    onClick = onTriggerFall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x44EF4444),
                        contentColor = RadarRed
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f).testTag("preset_fall_button")
                ) {
                    Text(
                        text = "4. Human Fall\n(Surge 48 dB)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        // Real-Time Continuous Perturbation Slider
        Column(modifier = Modifier.padding(top = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Variable Disturbance:",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "+${String.format("%.1f", manualSliderVal)} dB",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = RadarAmber
                )
            }

            Slider(
                value = manualSliderVal,
                onValueChange = {
                    manualSliderVal = it
                    onSliderDisturbance(it.toDouble())
                },
                valueRange = 0f..65f,
                colors = SliderDefaults.colors(
                    thumbColor = RadarAmber,
                    activeTrackColor = RadarAmber,
                    inactiveTrackColor = Color(0xFF1E293B)
                ),
                modifier = Modifier.testTag("manual_disturbance_slider")
            )
        }
    }
}
