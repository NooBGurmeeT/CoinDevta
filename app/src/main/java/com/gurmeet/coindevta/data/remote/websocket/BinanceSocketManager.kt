package com.gurmeet.coindevta.data.remote.websocket

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BinanceSocketManager @Inject constructor(
    private val client: OkHttpClient
) {

    fun observeAllPrices(): Flow<Pair<String, Double>> =
        callbackFlow {

            val request = Request.Builder()
                .url("wss://stream.binance.com:9443/ws/!miniTicker@arr")
                .build()

            val listener = object : WebSocketListener() {

                override fun onMessage(
                    webSocket: WebSocket,
                    text: String
                ) {
                    try {
                        val jsonArray = JSONArray(text)

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val symbol = obj.getString("s")
                            val price = obj.getString("c").toDouble()

                            trySend(symbol to price)
                        }
                    } catch (_: Exception) {}
                }
            }

            val socket = client.newWebSocket(request, listener)

            awaitClose {
                socket.close(1000, null)
            }
        }.buffer(capacity = 1000)
            .conflate()
}