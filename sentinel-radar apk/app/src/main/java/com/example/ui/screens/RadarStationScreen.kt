package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VolumetricState
import com.example.ui.RadarViewModel
import com.example.ui.components.CriticalFallBanner
import com.example.ui.components.DspFilterConfigCard
import com.example.ui.components.InteractivePerturbationCard
import com.example.ui.components.RadarOscilloscopeCanvas
import com.example.ui.components.VolumetricStatusCard
import com.example.ui.theme.RadarAmber
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarDarkBackground
import com.example.ui.theme.RadarEmerald
import com.example.ui.theme.RadarRed

@Composable
fun RadarStationScreen(
    viewModel: RadarViewModel,
    modifier: Modifier = Modifier
) {
    val chartData by viewModel.chartData.collectAsState()
    val telemetry by viewModel.currentTelemetry.collectAsState()
    val latchedAlert by viewModel.latchedFallAlert.collectAsState()
    val fallThreshold by viewModel.fallThreshold.collectAsState()
    val occupancyThreshold by viewModel.occupancyThreshold.collectAsState()
    val plotRaw by viewModel.plotRawAmplitude.collectAsState()
    val sourceMode by viewModel.dataSourceMode.collectAsState()
    val wsUrl by viewModel.wsUrl.collectAsState()
    val wsConnected by viewModel.webSocketClient.isConnected.collectAsState()
    val packetRate by viewModel.webSocketClient.currentPacketRate.collectAsState()

    val isEmergency = latchedAlert || telemetry.state == VolumetricState.FALL
    val displayRate = if (sourceMode == RadarViewModel.DataSourceMode.BUILTIN_DSP_SIMULATOR) 100 else packetRate
    val isLinkOnline = if (sourceMode == RadarViewModel.DataSourceMode.BUILTIN_DSP_SIMULATOR) true else wsConnected

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RadarDarkBackground)
    ) {
        // Critical Fall Transient Banner
        CriticalFallBanner(
            visible = latchedAlert,
            fallThreshold = fallThreshold,
            onDismiss = { viewModel.acknowledgeAndResetAlert() }
        )

        // Telemetry Sub-Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF060B19))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = "Radar Antenna",
                    tint = if (isEmergency) RadarRed else RadarCyan,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "802.11n HT20 CSI MULTIPATH RADAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "CHANNEL 6 (2.437 GHz) • 40 ACTIVE OFDM SUBCARRIERS",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sampling Rate Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "SAMPLING:",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "$displayRate Hz",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (displayRate >= 50) RadarEmerald else RadarAmber
                    )
                }

                // Link Online Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isLinkOnline) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Online",
                            tint = RadarEmerald,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (sourceMode == RadarViewModel.DataSourceMode.BUILTIN_DSP_SIMULATOR) "DSP ACTIVE" else "WS ONLINE",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = RadarEmerald
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Offline",
                            tint = RadarRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "WS OFFLINE",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = RadarRed
                        )
                    }
                }
            }
        }

        // Scrollable Body containing Live Oscilloscope & Controls
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Live Subcarrier Variance Oscilloscope
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0B132B))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LIVE SUBCARRIER VARIANCE (dB)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Real-Time Multipath Distortion Energy (1.0s Sliding Window)",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRACE:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isEmergency) RadarRed else RadarCyan)
                            )
                            Text(
                                text = if (isEmergency) "FALL" else "NOMINAL",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isEmergency) RadarRed else RadarCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    RadarOscilloscopeCanvas(
                        data = chartData,
                        fallThreshold = fallThreshold,
                        occupancyThreshold = occupancyThreshold,
                        plotRawAmplitude = plotRaw,
                        isEmergency = isEmergency,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .testTag("oscilloscope_canvas")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "PASSBAND: 0.15 - 6.0 Hz • 40 OFDM DUAL-POLARIZED",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "20 FPS DECOUPLED RENDER • LATENCY: <20ms",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // 2. Volumetric State Readout Card
            item {
                VolumetricStatusCard(
                    state = telemetry.state,
                    varianceDb = telemetry.varianceDb,
                    rawAmplitude = telemetry.rawAmplitude,
                    isEmergency = isEmergency,
                    actuateGpio = telemetry.actuateGpio,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. Interactive RF Channel Excitation (if in Simulation mode)
            if (sourceMode == RadarViewModel.DataSourceMode.BUILTIN_DSP_SIMULATOR) {
                item {
                    InteractivePerturbationCard(
                        onSetEmpty = { viewModel.setEmptyBaselineSimulation() },
                        onSetBreathing = { viewModel.setBreathingSimulation() },
                        onTriggerBookDrop = { viewModel.triggerBookDropSimulation() },
                        onTriggerFall = { viewModel.triggerFallSimulation() },
                        onSliderDisturbance = { viewModel.triggerManualDisturbance(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 4. DSP Filter & Threshold Configuration Card
            item {
                DspFilterConfigCard(
                    fallThreshold = fallThreshold,
                    occupancyThreshold = occupancyThreshold,
                    plotRawAmplitude = plotRaw,
                    dataSourceMode = sourceMode,
                    wsUrl = wsUrl,
                    onFallThresholdChange = { viewModel.setFallThreshold(it) },
                    onOccupancyThresholdChange = { viewModel.setOccupancyThreshold(it) },
                    onPlotRawChange = { viewModel.setPlotRawAmplitude(it) },
                    onDataSourceModeChange = { viewModel.setDataSourceMode(it) },
                    onWsUrlChange = { viewModel.updateWsUrl(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
