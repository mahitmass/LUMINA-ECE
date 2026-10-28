package com.example.data.engine

import com.example.data.model.CsiTelemetry
import com.example.data.model.VolumetricState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/**
 * High-Fidelity 100Hz DSP Simulation Engine implementing the exact multi-subcarrier
 * RF physics from Project Sentinel backend.py and Section 1-B / Section 6 specifications.
 */
class CsiDspSimulationEngine {
    private val _telemetryFlow = MutableSharedFlow<CsiTelemetry>(extraBufferCapacity = 64)
    val telemetryFlow = _telemetryFlow.asSharedFlow()

    private var simulationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Current simulation mode
    enum class SimulationMode {
        EMPTY_BASELINE,
        BREATHING_MICRO_DOPPLER,
        DROPPED_BOOK_TEST,
        FALL_EVENT_TEST,
        LIVE_INTERACTIVE
    }

    private var currentMode = SimulationMode.EMPTY_BASELINE
    private var interactiveDisturbanceLevel = 0.0 // 0.0 to 60.0 dB
    private var transientPulseEnergy = 0.0
    private var transientPulseDecay = 0.92
    private var sampleCounter = 0
    private var lastAlarmTime = 0L

    // Configurable thresholds matching backend.py
    var fallThreshold = 30.0
    var occupancyThreshold = 2.0

    // Simulating 40 clean OFDM subcarriers
    private val subcarrierNoiseBases = DoubleArray(40) { 18.0 + Random.nextDouble(-4.0, 4.0) }

    fun start() {
        if (simulationJob?.isActive == true) return
        simulationJob = scope.launch {
            while (isActive) {
                val telemetry = generateNextSample()
                _telemetryFlow.tryEmit(telemetry)
                // 10ms delay = 100 Hz deterministic sampling rate (FR-01)
                delay(10)
            }
        }
    }

    fun stop() {
        simulationJob?.cancel()
        simulationJob = null
    }

    fun setMode(mode: SimulationMode) {
        currentMode = mode
        if (mode == SimulationMode.DROPPED_BOOK_TEST) {
            // Section 6 Step 3: Dropped book spikes variance at 6-12 dB
            transientPulseEnergy = Random.nextDouble(8.0, 11.5)
            transientPulseDecay = 0.90 // quick decay ~300ms
        } else if (mode == SimulationMode.FALL_EVENT_TEST) {
            // Section 6 Step 3: Rapid human fall spikes 90th percentile above 35-60 dB
            transientPulseEnergy = Random.nextDouble(42.0, 58.0)
            transientPulseDecay = 0.97 // longer decay ~2.5s
        }
    }

    fun triggerTransient(energyDb: Double) {
        transientPulseEnergy = energyDb
        transientPulseDecay = if (energyDb >= fallThreshold) 0.96 else 0.91
    }

    fun setInteractiveDisturbance(levelDb: Double) {
        currentMode = SimulationMode.LIVE_INTERACTIVE
        interactiveDisturbanceLevel = levelDb
    }

    private fun generateNextSample(): CsiTelemetry {
        sampleCounter++
        val t = sampleCounter * 0.01 // time in seconds

        // Decay transient if present
        if (transientPulseEnergy > 0.05) {
            transientPulseEnergy *= transientPulseDecay
        } else {
            transientPulseEnergy = 0.0
            if (currentMode == SimulationMode.DROPPED_BOOK_TEST || currentMode == SimulationMode.FALL_EVENT_TEST) {
                currentMode = SimulationMode.EMPTY_BASELINE
            }
        }

        // Generate baseline multi-subcarrier amplitudes & variance
        var baseVariance: Double
        val rawAmpBase = 24.5 + sin(t * 0.1) * 0.8 + Random.nextDouble(-0.3, 0.3)

        when (currentMode) {
            SimulationMode.EMPTY_BASELINE -> {
                // Section 6 Step 3: Empty room baseline is flat between 0.2 - 1.2 dB
                baseVariance = 0.45 + sin(t * 0.5) * 0.25 + Random.nextDouble(0.0, 0.35)
            }
            SimulationMode.BREATHING_MICRO_DOPPLER -> {
                // Breathing cycle: ~0.25 Hz sine wave, variance between 2.2 - 4.8 dB
                val breathingCycle = (sin(2.0 * Math.PI * 0.25 * t) + 1.0) / 2.0
                baseVariance = 1.8 + breathingCycle * 2.2 + Random.nextDouble(0.0, 0.5)
            }
            SimulationMode.DROPPED_BOOK_TEST -> {
                baseVariance = 0.6 + transientPulseEnergy + Random.nextDouble(0.0, 0.4)
            }
            SimulationMode.FALL_EVENT_TEST -> {
                baseVariance = 0.8 + transientPulseEnergy + Random.nextDouble(-1.0, 1.5)
            }
            SimulationMode.LIVE_INTERACTIVE -> {
                baseVariance = 0.4 + interactiveDisturbanceLevel + Random.nextDouble(0.0, 0.6)
            }
        }

        // Add any remaining transient pulse
        val finalVariance = (baseVariance + (if (currentMode != SimulationMode.DROPPED_BOOK_TEST && currentMode != SimulationMode.FALL_EVENT_TEST) transientPulseEnergy else 0.0))
            .coerceAtLeast(0.1)

        // Evaluate Volumetric State
        val evaluatedState = when {
            finalVariance >= fallThreshold -> VolumetricState.FALL
            finalVariance >= occupancyThreshold -> VolumetricState.BREATHING
            else -> VolumetricState.EMPTY
        }

        // Hardware Actuation: hold GPIO 25 high for 3.0 seconds on fall (Section 1-B / FR-05)
        val now = System.currentTimeMillis()
        if (evaluatedState == VolumetricState.FALL) {
            lastAlarmTime = now
        }
        val actuateGpio = (now - lastAlarmTime) < 3000L && lastAlarmTime > 0L

        return CsiTelemetry(
            state = evaluatedState,
            varianceDb = ((finalVariance * 10).toInt() / 10.0),
            rawAmplitude = ((rawAmpBase * 10).toInt() / 10.0),
            packetRate = 100,
            actuateGpio = actuateGpio,
            timestamp = now
        )
    }
}
