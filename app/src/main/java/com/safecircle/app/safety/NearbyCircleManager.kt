package com.safecircle.app.safety

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.safecircle.app.data.SafeCirclePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID

class NearbyCircleManager(private val context: Context) {
    private val prefs = SafeCirclePreferences(context)
    private val client = Nearby.getConnectionsClient(context)
    private val _peerCount = MutableStateFlow(0)
    val peerCount = _peerCount.asStateFlow()
    private val connected = mutableSetOf<String>()
    private val endpointNames = mutableMapOf<String, String>()
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            if (payload.type != Payload.Type.BYTES) return
            try {
                val json = JSONObject(String(bytes, StandardCharsets.UTF_8))
                if (json.optString("type") == "handshake") {
                    val accepted = json.optString("code") == prefs.circleCode
                    if (!accepted) client.disconnectFromEndpoint(endpointId)
                } else if (json.optString("type") == "sos" && json.optString("code") == prefs.circleCode) {
                    // The app layer can surface this event in a later iteration.
                    // The transport is intentionally kept independent from UI.
                }
            } catch (_: Exception) { }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private val connectionLifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            client.acceptConnection(endpointId, payloadCallback)
            endpointNames[endpointId] = info.endpointName
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connected.add(endpointId)
                _peerCount.value = connected.size
                sendHandshake(endpointId)
            }
        }

        override fun onDisconnected(endpointId: String) {
            connected.remove(endpointId)
            endpointNames.remove(endpointId)
            _peerCount.value = connected.size
        }
    }

    fun start() {
        val name = prefs.name.ifBlank { "SafeCircle user" }
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        client.startAdvertising(name, SERVICE_ID, connectionLifecycle, options)
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        client.startDiscovery(SERVICE_ID, object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                client.requestConnection(name, endpointId, connectionLifecycle)
            }
            override fun onEndpointLost(endpointId: String) = Unit
        }, discoveryOptions)
    }

    fun stop() {
        client.stopAdvertising()
        client.stopDiscovery()
        connected.toList().forEach(client::disconnectFromEndpoint)
        connected.clear()
        _peerCount.value = 0
    }

    fun broadcastSos(latitude: Double?, longitude: Double?) {
        val json = JSONObject().apply {
            put("type", "sos")
            put("code", prefs.circleCode)
            put("sender", prefs.name)
            put("timestamp", System.currentTimeMillis())
            if (latitude != null) put("latitude", latitude)
            if (longitude != null) put("longitude", longitude)
            put("message", "SafeCircle SOS: I may need help.")
            put("id", UUID.randomUUID().toString())
        }
        val payload = Payload.fromBytes(json.toString().toByteArray(StandardCharsets.UTF_8))
        connected.forEach { client.sendPayload(it, payload) }
    }

    private fun sendHandshake(endpointId: String) {
        val json = JSONObject().apply {
            put("type", "handshake")
            put("code", prefs.circleCode)
        }
        client.sendPayload(endpointId, Payload.fromBytes(json.toString().toByteArray(StandardCharsets.UTF_8)))
    }

    companion object {
        private const val SERVICE_ID = "com.safecircle.app.circle"
    }
}
