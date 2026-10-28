package com.example.data.model

data class CsiPoint(
    val index: Int,
    val varianceDb: Double,
    val rawAmplitude: Double
)

enum class VolumetricState(val rawName: String, val displayName: String) {
    EMPTY("empty", "NOMINAL / EMPTY"),
    BREATHING("breathing", "OCCUPIED (MICRO-DOPPLER)"),
    FALL("fall", "FALL TRANSIENT");

    companion object {
        fun fromRaw(raw: String): VolumetricState {
            return entries.firstOrNull { it.rawName.equals(raw, ignoreCase = true) } ?: EMPTY
        }
    }
}

data class CsiTelemetry(
    val state: VolumetricState = VolumetricState.EMPTY,
    val varianceDb: Double = 0.0,
    val rawAmplitude: Double = 0.0,
    val packetRate: Int = 0,
    val actuateGpio: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class RadarEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "FALL", "MICRO-DOPPLER", "BOOK_DROP", "EMPTY_CALIBRATION"
    val peakVariance: Double,
    val durationSec: Double = 0.0,
    val description: String,
    val isEmergency: Boolean = false
)

data class FresnelParams(
    val totalDistanceMeters: Double = 3.0,
    val targetDistanceMeters: Double = 1.5,
    val frequencyGhz: Double = 2.437,
    val heightMeters: Double = 1.0
) {
    // Speed of light c = 3e8 m/s, wavelength lambda = c / f
    val wavelengthMeters: Double
        get() = 0.299792458 / frequencyGhz

    // r1 = sqrt((lambda * d1 * d2) / d)
    val firstFresnelRadiusCm: Double
        get() {
            val d1 = targetDistanceMeters.coerceAtLeast(0.1)
            val d = totalDistanceMeters.coerceAtLeast(d1 + 0.1)
            val d2 = d - d1
            val rMeters = kotlin.math.sqrt((wavelengthMeters * d1 * d2) / d)
            return rMeters * 100.0
        }
}
