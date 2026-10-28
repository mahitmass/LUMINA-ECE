package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.ui.RadarViewModel
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarRed

@Composable
fun DspFilterConfigCard(
    fallThreshold: Double,
    occupancyThreshold: Double,
    plotRawAmplitude: Boolean,
    dataSourceMode: RadarViewModel.DataSourceMode,
    wsUrl: String,
    onFallThresholdChange: (Double) -> Unit,
    onOccupancyThresholdChange: (Double) -> Unit,
    onPlotRawChange: (Boolean) -> Unit,
    onDataSourceModeChange: (RadarViewModel.DataSourceMode) -> Unit,
    onWsUrlChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingUrl by remember(wsUrl) { mutableStateOf(wsUrl) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0B132B))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(16.dp)
            .testTag("dsp_filter_config_card"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "DSP Config",
                tint = RadarCyan,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "DSP FILTER & THRESHOLD CONFIG",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.2.sp
            )
        }

        // Data Source Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF020617))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isSim = dataSourceMode == RadarViewModel.DataSourceMode.BUILTIN_DSP_SIMULATOR
            BoxButton(
                title = "Built-in 100Hz DSP Engine",
                selected = isSim,
                onClick = { onDataSourceModeChange(RadarViewModel.DataSourceMode.BUILTIN_DSP_SIMULATOR) },
                modifier = Modifier.weight(1f)
            )
            BoxButton(
                title = "Live FastAPI WebSocket",
                selected = !isSim,
                onClick = { onDataSourceModeChange(RadarViewModel.DataSourceMode.LIVE_WEBSOCKET) },
                modifier = Modifier.weight(1f)
            )
        }

        // Fall Threshold Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fall Alert Threshold",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFCBD5E1)
                )
                Text(
                    text = "${String.format("%.1f", fallThreshold)} dB",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = RadarRed
                )
            }
            Slider(
                value = fallThreshold.toFloat(),
                onValueChange = { onFallThresholdChange(it.toDouble()) },
                valueRange = 5.0f..80.0f,
                steps = 149,
                colors = SliderDefaults.colors(
                    thumbColor = RadarRed,
                    activeTrackColor = RadarRed,
                    inactiveTrackColor = Color(0xFF1E293B)
                ),
                modifier = Modifier.testTag("fall_threshold_slider")
            )
        }

        // Micro-Doppler Threshold Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Micro-Doppler Threshold",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFCBD5E1)
                )
                Text(
                    text = "${String.format("%.1f", occupancyThreshold)} dB",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = RadarCyan
                )
            }
            Slider(
                value = occupancyThreshold.toFloat(),
                onValueChange = { onOccupancyThresholdChange(it.toDouble()) },
                valueRange = 0.5f..15.0f,
                steps = 29,
                colors = SliderDefaults.colors(
                    thumbColor = RadarCyan,
                    activeTrackColor = RadarCyan,
                    inactiveTrackColor = Color(0xFF1E293B)
                ),
                modifier = Modifier.testTag("micro_doppler_slider")
            )
        }

        HorizontalDivider(color = Color(0xFF1E293B))

        // Signal Overlay Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Overlay Raw CSI Amplitude",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFCBD5E1)
                )
                Text(
                    text = "Displays unbandpassed mean subcarrier signal",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64748B)
                )
            }
            Switch(
                checked = plotRawAmplitude,
                onCheckedChange = onPlotRawChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = RadarCyan,
                    checkedTrackColor = Color(0x3322D3EE),
                    uncheckedThumbColor = Color(0xFF64748B),
                    uncheckedTrackColor = Color(0xFF1E293B)
                ),
                modifier = Modifier.testTag("raw_overlay_toggle")
            )
        }

        if (dataSourceMode == RadarViewModel.DataSourceMode.LIVE_WEBSOCKET) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "FastAPI WebSocket URI (ws://...)",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editingUrl,
                        onValueChange = { editingUrl = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadarCyan,
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ws_url_input")
                    )
                    Button(
                        onClick = { onWsUrlChange(editingUrl) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RadarCyan,
                            contentColor = Color(0xFF020617)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.testTag("ws_connect_button")
                    ) {
                        Text(
                            text = "Connect",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // DSP Filter Specs Readout Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF020617))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "FILTER: 4th-Order Butterworth SOS",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = "PASSBAND: 0.15 Hz – 6.0 Hz (Breathing & Fall)",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = "SUBCARRIERS: 40 Clean OFDM (Indices 6..25, 38..57)",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = "VARIANCE METRIC: 90th Percentile / Sliding Window",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun BoxButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) Color(0xFF1E293B) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) RadarCyan else Color(0xFF64748B)
        )
    }
}
