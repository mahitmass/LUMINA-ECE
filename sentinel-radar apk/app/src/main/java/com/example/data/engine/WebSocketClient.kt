package com.example.data.engine

import com.example.data.model.CsiTelemetry
import com.example.data.model.VolumetricState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class WebSocketClient {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _telemetryFlow = MutableSharedFlow<CsiTelemetry>(extraBufferCapacity = 128)
    val telemetryFlow = _telemetryFlow.asSharedFlow()

    private val _currentPacketRate = MutableStateFlow(0)
    val currentPacketRate = _currentPacketRate.asStateFlow()

    private val packetCounter = AtomicInteger(0)
    private var lastRateCalcTime = System.currentTimeMillis()
    private var targetUrl = "ws://10.0.2.2:8000/ws/radar"

    private var isManualDisconnect = false

    init {
        // Coroutine to compute packet sampling rate (Hz) every second
        scope.launch {
            while (isActive) {
                delay(1000)
                val count = packetCounter.getAndSet(0)
                _currentPacketRate.value = count
            }
        }
    }

    fun connect(url: String) {
        targetUrl = url
        isManualDisconnect = false
        disconnectInternal()
        startConnection()
    }

    fun disconnect() {
        isManualDisconnect = true
        reconnectJob?.cancel()
        disconnectInternal()
    }

    private fun disconnectInternal() {
        try {
            webSocket?.close(1000, "Normal Closure")
        } catch (_: Exception) {}
        webSocket = null
        _isConnected.value = false
    }

    private fun startConnection() {
        try {
            val request = Request.Builder().url(targetUrl).build()
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    _isConnected.value = true
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    packetCounter.incrementAndGet()
                    parsePacket(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                    _isConnected.value = false
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _isConnected.value = false
                    scheduleReconnect()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    _isConnected.value = false
                    scheduleReconnect()
                }
            })
        } catch (_: Exception) {
            _isConnected.value = false
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (isManualDisconnect) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(3000)
            if (!isManualDisconnect && !_isConnected.value) {
                startConnection()
            }
        }
    }

    private fun parsePacket(jsonStr: String) {
        try {
            val obj = JSONObject(jsonStr)
            val rawState = obj.optString("state", "empty")
            val variance = obj.optDouble("variance_db", 0.0)
            val rawAmp = obj.optDouble("raw_amplitude", 0.0)
            val actuate = obj.optBoolean("actuate_gpio", false)

            val telemetry = CsiTelemetry(
                state = VolumetricState.fromRaw(rawState),
                varianceDb = variance,
                rawAmplitude = rawAmp,
                packetRate = _currentPacketRate.value,
                actuateGpio = actuate,
                timestamp = System.currentTimeMillis()
            )
            _telemetryFlow.tryEmit(telemetry)
        } catch (_: Exception) {}
    }
}
