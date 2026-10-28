package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.data.model.RadarEvent
import com.example.ui.RadarViewModel
import com.example.ui.theme.RadarAmber
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarDarkBackground
import com.example.ui.theme.RadarEmerald
import com.example.ui.theme.RadarRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IncidentLogScreen(
    viewModel: RadarViewModel,
    modifier: Modifier = Modifier
) {
    val events by viewModel.incidentEvents.collectAsState()
    val fallCount = events.count { it.isEmergency }
    val maxVariance = events.maxOfOrNull { it.peakVariance } ?: 0.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RadarDarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Card
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Incident Logs",
                            tint = RadarCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "INCIDENT AUDIT & TELEMETRY LOG",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.8.sp
                        )
                    }

                    if (events.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.clearIncidentEvents() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color(0xFF94A3B8)
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.testTag("clear_logs_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(text = "Clear", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBox(
                        title = "TOTAL EVENTS",
                        value = "${events.size}",
                        color = RadarCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        title = "CRITICAL FALLS",
                        value = "$fallCount",
                        color = if (fallCount > 0) RadarRed else RadarEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        title = "PEAK VARIANCE",
                        value = "${String.format("%.1f", maxVariance)} dB",
                        color = RadarAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // List of Events
        if (events.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0B132B))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "No Incidents",
                        tint = RadarEmerald,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "No Incidents Recorded",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Fall transients and benchmark test triggers will be logged here with timestamps and peak dielectric variance.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            items(events) { event ->
                IncidentEventCard(event = event)
            }
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF020617))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) {
        Text(
            text = title,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}

@Composable
private fun IncidentEventCard(
    event: RadarEvent
) {
    val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    val timeStr = dateFormat.format(Date(event.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (event.isEmergency) Color(0x22EF4444) else Color(0xFF0B132B))
            .border(
                width = 1.dp,
                color = if (event.isEmergency) RadarRed else Color(0xFF1E293B),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (event.isEmergency) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Emergency",
                        tint = RadarRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = event.type,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (event.isEmergency) RadarRed else RadarCyan
                )
            }

            Text(
                text = timeStr,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF94A3B8)
            )
        }

        Text(
            text = event.description,
            fontSize = 11.sp,
            color = Color(0xFFCBD5E1),
            lineHeight = 15.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Peak: ${String.format("%.1f", event.peakVariance)} dB",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (event.isEmergency) RadarRed else RadarAmber
            )
            if (event.durationSec > 0) {
                Text(
                    text = "Alarm Duration: ${event.durationSec}s",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
