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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarRed
import com.example.ui.theme.RadarRedDeep

@Composable
fun VolumetricStatusCard(
    state: VolumetricState,
    varianceDb: Double,
    rawAmplitude: Double,
    isEmergency: Boolean,
    actuateGpio: Boolean,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isEmergency) Color(0x33450A0A) else Color(0xFF0B132B)
    val cardBorder = if (isEmergency) RadarRed else Color(0xFF1E293B)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
            .padding(16.dp)
            .testTag("volumetric_status_card")
    ) {
        Text(
            text = "VOLUMETRIC STATE READOUT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // State Pill Badge
        Row(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when {
                        isEmergency || state == VolumetricState.FALL -> Color(0x33EF4444)
                        state == VolumetricState.BREATHING -> Color(0x2622D3EE)
                        else -> Color(0xFF1E293B)
                    }
                )
                .border(
                    width = 1.dp,
                    color = when {
                        isEmergency || state == VolumetricState.FALL -> RadarRed
                        state == VolumetricState.BREATHING -> RadarCyan
                        else -> Color(0xFF334155)
                    },
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                isEmergency || state == VolumetricState.FALL -> {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Fall Emergency",
                        tint = RadarRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "FALL TRANSIENT",
                        color = RadarRed,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
                state == VolumetricState.BREATHING -> {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Micro-Doppler Breathing",
                        tint = RadarCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "OCCUPIED (MICRO-DOPPLER)",
                        color = RadarCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.8.sp
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Nominal",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "NOMINAL / EMPTY",
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

        // Monospace Data Readouts
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column {
                Text(
                    text = "SUBCARRIER VARIANCE (90TH PERCENTILE)",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "Variance: ${String.format("%.1f", varianceDb)} dB",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isEmergency) RadarRed else RadarCyan
                )
            }

            Column {
                Text(
                    text = "MEAN SUBCARRIER MAGNITUDE",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "Amplitude: ${String.format("%.1f", rawAmplitude)} dB",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFE2E8F0)
                )
            }

            HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.8.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Actuator",
                        tint = if (actuateGpio) RadarRed else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "GPIO 25 Actuators (FR-05):",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8)
                    )
                }
                Text(
                    text = if (actuateGpio) "ACTIVE (5V HIGH)" else "STANDBY (LOW)",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (actuateGpio) RadarRed else Color(0xFF64748B)
                )
            }
        }
    }
}
