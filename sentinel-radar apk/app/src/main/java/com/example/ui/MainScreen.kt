package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.ExhibitionDemoScreen
import com.example.ui.screens.FirmwareCodeScreen
import com.example.ui.screens.HardwareWiringScreen
import com.example.ui.screens.IncidentLogScreen
import com.example.ui.screens.RadarStationScreen
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarDarkBackground
import com.example.ui.theme.RadarEmerald
import com.example.ui.theme.RadarRed

enum class SentinelTab(val label: String, val icon: ImageVector, val tag: String) {
    RADAR("Radar", Icons.Default.Sensors, "nav_radar_tab"),
    DEMO("Exhibition", Icons.Default.Shield, "nav_demo_tab"),
    HARDWARE("Wiring", Icons.Default.ElectricalServices, "nav_hardware_tab"),
    CODE("Firmware", Icons.Default.Code, "nav_code_tab"),
    LOGS("Incidents", Icons.Default.History, "nav_logs_tab")
}

@Composable
fun MainScreen(
    viewModel: RadarViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(SentinelTab.RADAR) }
    val latchedAlert by viewModel.latchedFallAlert.collectAsState()
    val telemetry by viewModel.currentTelemetry.collectAsState()
    val isEmergency = latchedAlert || telemetry.actuateGpio

    // Handle back button on sub-screens to return to Radar
    if (selectedTab != SentinelTab.RADAR) {
        BackHandler {
            selectedTab = SentinelTab.RADAR
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(RadarDarkBackground),
        containerColor = RadarDarkBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(
                        width = 1.dp,
                        color = Color(0xFF1E293B)
                    )
                    .testTag("bottom_nav_bar"),
                containerColor = Color(0xFF020617),
                tonalElevation = 8.dp
            ) {
                SentinelTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (isEmergency && tab == SentinelTab.RADAR) RadarRed else RadarCyan,
                            selectedTextColor = if (isEmergency && tab == SentinelTab.RADAR) RadarRed else RadarCyan,
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B),
                            indicatorColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top App Bar per Sentinel specification
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .background(Color(0xFF020617))
                    .border(1.dp, Color(0xFF1E293B))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isEmergency) Color(0x33EF4444) else Color(0x1A22D3EE))
                            .border(1.dp, if (isEmergency) RadarRed else RadarCyan, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = "Project Sentinel",
                            tint = if (isEmergency) RadarRed else RadarCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "CSI Radar Station",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "PROJECT SENTINEL // 802.11n HT20 MULTIPATH DOPPLER ENGINE",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // GPIO 25 Actuator Live Status Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isEmergency) Color(0x33EF4444) else Color(0xFF0F172A))
                        .border(1.dp, if (isEmergency) RadarRed else Color(0xFF1E293B), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isEmergency) RadarRed else RadarEmerald)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = if (isEmergency) "GPIO 25 HIGH" else "GPIO 25 LOW",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isEmergency) RadarRed else RadarEmerald,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Screen content
            Crossfade(
                targetState = selectedTab,
                label = "screen_crossfade",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    SentinelTab.RADAR -> RadarStationScreen(viewModel = viewModel)
                    SentinelTab.DEMO -> ExhibitionDemoScreen(viewModel = viewModel)
                    SentinelTab.HARDWARE -> HardwareWiringScreen(viewModel = viewModel)
                    SentinelTab.CODE -> FirmwareCodeScreen()
                    SentinelTab.LOGS -> IncidentLogScreen(viewModel = viewModel)
                }
            }
        }
    }
}
