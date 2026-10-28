package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.example.ui.RadarViewModel
import com.example.ui.components.CriticalFallBanner
import com.example.ui.theme.RadarAmber
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarDarkBackground
import com.example.ui.theme.RadarEmerald
import com.example.ui.theme.RadarRed

@Composable
fun ExhibitionDemoScreen(
    viewModel: RadarViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.currentTelemetry.collectAsState()
    val latchedAlert by viewModel.latchedFallAlert.collectAsState()
    val fallThreshold by viewModel.fallThreshold.collectAsState()
    val stage by viewModel.demoStage.collectAsState()
    val stageState by viewModel.demoStageState.collectAsState()
    val statusMessage by viewModel.demoStatusMessage.collectAsState()
    val latencyMs by viewModel.demoLatencyMs.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RadarDarkBackground)
    ) {
        // Critical Fall Banner if latched
        CriticalFallBanner(
            visible = latchedAlert,
            fallThreshold = fallThreshold,
            onDismiss = { viewModel.acknowledgeAndResetAlert() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0B132B))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Exhibition Protocol",
                            tint = RadarCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "THREE-STAGE LIVE JUDGE DEMONSTRATION",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Text(
                        text = "Automated test harness implementing Section 6 of the Project Sentinel specification. Demonstrates ambient multipath rejection, small-object false-positive immunity, and low-latency (<400ms) through-wall fall latching.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 16.sp
                    )

                    HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 4.dp))

                    // Live Status Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF020617))
                            .border(
                                1.dp,
                                when (stageState) {
                                    RadarViewModel.DemoStepState.PASSED -> RadarEmerald
                                    RadarViewModel.DemoStepState.FAILED -> RadarRed
                                    RadarViewModel.DemoStepState.RUNNING -> RadarCyan
                                    else -> Color(0xFF1E293B)
                                },
                                RoundedCornerShape(4.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (stageState == RadarViewModel.DemoStepState.RUNNING) {
                            CircularProgressIndicator(
                                color = RadarCyan,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (stageState == RadarViewModel.DemoStepState.PASSED) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Passed",
                                tint = RadarEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                        } else if (stageState == RadarViewModel.DemoStepState.FAILED) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Failed",
                                tint = RadarRed,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = "Idle",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BENCHMARK STATUS",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = statusMessage,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = when (stageState) {
                                    RadarViewModel.DemoStepState.PASSED -> RadarEmerald
                                    RadarViewModel.DemoStepState.FAILED -> RadarRed
                                    RadarViewModel.DemoStepState.RUNNING -> RadarCyan
                                    else -> Color(0xFFCBD5E1)
                                }
                            )
                        }
                    }

                    // Live Telemetry Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Live Variance: ${String.format("%.1f", telemetry.varianceDb)} dB",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (telemetry.varianceDb >= fallThreshold) RadarRed else RadarCyan
                        )
                        Text(
                            text = "Detection Latency: ${latencyMs} ms",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (latencyMs > 0 && latencyMs < 400) RadarEmerald else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // STAGE 1: Empty Room Baseline
            item {
                DemoStageCard(
                    stageNum = 1,
                    title = "Stage 1: Empty Room Baseline",
                    description = "Clears Fresnel zone behind partition for 5 seconds. Verifies the cyan waveform stays flat between 0.2 – 1.2 dB without false drift.",
                    metricCriteria = "Max Variance <= 1.2 dB • Nominal State",
                    isActive = stage == 1,
                    isRunning = stage == 1 && stageState == RadarViewModel.DemoStepState.RUNNING,
                    isPassed = stage == 1 && stageState == RadarViewModel.DemoStepState.PASSED,
                    onExecute = { viewModel.runDemoStage(1) },
                    testTag = "demo_stage_1_button"
                )
            }

            // STAGE 2: Dropped Book False-Positive Rejection
            item {
                DemoStageCard(
                    stageNum = 2,
                    title = "Stage 2: False-Positive Rejection (Dropped Book)",
                    description = "Simulates dropping a heavy book behind the barrier. Dielectric constant (εr ≈ 2.5) creates a 6 – 12 dB spike — strictly below the 30 dB threshold.",
                    metricCriteria = "Transient Peak < 30.0 dB • Zero False Alarms",
                    isActive = stage == 2,
                    isRunning = stage == 2 && stageState == RadarViewModel.DemoStepState.RUNNING,
                    isPassed = stage == 2 && stageState == RadarViewModel.DemoStepState.PASSED,
                    onExecute = { viewModel.runDemoStage(2) },
                    testTag = "demo_stage_2_button"
                )
            }

            // STAGE 3: Through-Wall Fall Detection
            item {
                DemoStageCard(
                    stageNum = 3,
                    title = "Stage 3: Through-Wall Rapid Fall Detection",
                    description = "Simulates rapid human fall (εr ≈ 45 water tissue) across Fresnel zones. Spikes 90th percentile subcarrier variance above 35 – 60 dB in <400ms.",
                    metricCriteria = "Variance >= 30.0 dB • Latency < 400ms • Actuator 5V High",
                    isActive = stage == 3,
                    isRunning = stage == 3 && stageState == RadarViewModel.DemoStepState.RUNNING,
                    isPassed = stage == 3 && stageState == RadarViewModel.DemoStepState.PASSED,
                    onExecute = { viewModel.runDemoStage(3) },
                    testTag = "demo_stage_3_button"
                )
            }
        }
    }
}

@Composable
private fun DemoStageCard(
    stageNum: Int,
    title: String,
    description: String,
    metricCriteria: String,
    isActive: Boolean,
    isRunning: Boolean,
    isPassed: Boolean,
    onExecute: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0B132B))
            .border(
                width = 1.dp,
                color = when {
                    isPassed -> RadarEmerald
                    isActive -> RadarCyan
                    else -> Color(0xFF1E293B)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPassed) RadarEmerald else Color.White
            )

            if (isPassed) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Passed",
                        tint = RadarEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "VERIFIED",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = RadarEmerald
                    )
                }
            }
        }

        Text(
            text = description,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            lineHeight = 15.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF020617))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Target: $metricCriteria",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = RadarAmber
            )
        }

        Button(
            onClick = onExecute,
            enabled = !isRunning,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (stageNum == 3) RadarRed else RadarCyan,
                contentColor = if (stageNum == 3) Color.White else Color(0xFF020617)
            ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(text = "Executing Test...", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Execute Demonstration #$stageNum",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
