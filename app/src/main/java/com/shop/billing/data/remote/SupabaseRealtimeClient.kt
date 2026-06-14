package com.shop.billing.data.remote

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class RealtimeChange(
    val type: String,
    val table: String,
    val new: JSONObject?,
    val old: JSONObject?
)

@Singleton
class SupabaseRealtimeClient @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    private var webSocket: WebSocket? = null
    private var ref = 0
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var scope: CoroutineScope? = null
    private var connectionUrl: String = ""
    private var subscribedTables: List<String> = emptyList()

    private val _events = MutableSharedFlow<RealtimeChange>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeChange> = _events

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    fun connect(url: String, apiKey: String, tables: List<String> = listOf(
        "bills", "bill_items", "shop_items", "customers", "customer_payments"
    )) {
        disconnect()
        _connected.value = false
        subscribedTables = tables
        connectionUrl = url
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/') + "/realtime/v1/websocket?apikey=$apiKey&vsn=1.0.0"
        scope = CoroutineScope(Dispatchers.IO + Job())
        doConnect()
    }

    private fun doConnect() {
        Log.d(TAG, "Connecting to Realtime: $connectionUrl")
        val request = Request.Builder().url(connectionUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "Realtime WebSocket opened")
                _connected.value = true
                ref = 0
                for (table in subscribedTables) {
                    subscribeToTable(ws, table)
                }
                startHeartbeat(ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Realtime WebSocket failure: ${t.message}")
                _connected.value = false
                scheduleReconnect()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Realtime WebSocket closed: $code $reason")
                _connected.value = false
                scheduleReconnect()
            }
        })
    }

    private fun subscribeToTable(ws: WebSocket, table: String) {
        ref++
        val msg = JSONObject().apply {
            put("topic", "realtime:$table")
            put("event", "phx_join")
            put("payload", JSONObject().apply {
                put("config", JSONObject())
            })
            put("ref", ref.toString())
        }
        ws.send(msg.toString())
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val event = json.optString("event", "")
            when (event) {
                "phx_reply" -> {
                    val status = json.optJSONObject("payload")?.optString("status", "")
                    if (status == "ok") {
                        Log.d(TAG, "Subscribed to ${json.optString("topic")}")
                    }
                }
                "postgres_changes" -> {
                    val payload = json.optJSONObject("payload") ?: return
                    val change = RealtimeChange(
                        type = payload.optString("type", ""),
                        table = payload.optString("table", ""),
                        new = payload.optJSONObject("new"),
                        old = payload.optJSONObject("old")
                    )
                    _events.tryEmit(change)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Realtime message", e)
        }
    }

    private fun startHeartbeat(ws: WebSocket) {
        stopHeartbeat()
        heartbeatJob = scope?.launch {
            while (isActive) {
                delay(25000)
                ref++
                val msg = JSONObject().apply {
                    put("topic", "phoenix")
                    put("event", "heartbeat")
                    put("payload", JSONObject())
                    put("ref", ref.toString())
                }
                ws.send(msg.toString())
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun scheduleReconnect() {
        stopHeartbeat()
        reconnectJob?.cancel()
        reconnectJob = scope?.launch {
            delay(5000)
            Log.d(TAG, "Reconnecting to Realtime...")
            doConnect()
        }
    }

    fun disconnect() {
        _connected.value = false
        reconnectJob?.cancel()
        reconnectJob = null
        stopHeartbeat()
        webSocket?.close(1000, "Client closing")
        webSocket = null
        scope?.coroutineContext?.let {
            val job = it[Job]
            job?.cancel()
        }
        scope = null
    }

    companion object {
        private const val TAG = "SupabaseRealtime"
    }
}
