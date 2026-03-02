package com.gurmeet.coindevta.data.remote.websocket

import android.util.Log
import com.gurmeet.coindevta.domain.model.TickerUpdate
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import okhttp3.*
import okio.ByteString
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BinanceSocketManager @Inject constructor(
    private val client: OkHttpClient
) {

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // --------------------------------------------------
    // Shared hot flow (single socket for entire app)
    // --------------------------------------------------

    private val sharedTickerFlow: SharedFlow<TickerUpdate> by lazy {
        createSocketFlow()
            .buffer(capacity = 5000)
            .shareIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                replay = 0
            )
    }

    fun observeAllPrices(): Flow<TickerUpdate> = sharedTickerFlow

    // --------------------------------------------------
    // Internal socket creator (auto reconnect)
    // --------------------------------------------------

    private fun createSocketFlow(): Flow<TickerUpdate> {

        return callbackFlow {

            Log.d("Socket", "Creating WebSocket connection")

            val request = Request.Builder()
                .url("wss://stream.binance.com:9443/ws/!miniTicker@arr")
                .build()

            val listener = object : WebSocketListener() {

                override fun onOpen(
                    webSocket: WebSocket,
                    response: Response
                ) {
                    Log.d("Socket", "WebSocket connected")
                }

                override fun onMessage(
                    webSocket: WebSocket,
                    text: String
                ) {
                    try {
                        val jsonArray = JSONArray(text)

                        for (i in 0 until jsonArray.length()) {

                            val obj = jsonArray.getJSONObject(i)

                            val symbol = obj.optString("s")
                            val currentPrice = obj.optDouble("c")
                            val openPrice24h = obj.optDouble("o")

                            trySend(
                                TickerUpdate(
                                    symbol = symbol,
                                    currentPrice = currentPrice,
                                    openPrice24h = openPrice24h,
                                    isPositive24h = currentPrice >= openPrice24h
                                )
                            )
                        }

                    } catch (e: Exception) {
                        Log.e("Socket", "Parse error: ${e.message}")
                    }
                }

                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?
                ) {
                    Log.e("Socket", "WebSocket failure: ${t.message}")
                    close(t) // 👈 THIS triggers retry
                }

                override fun onClosed(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String
                ) {
                    Log.d("Socket", "WebSocket closed")
                    close() // 👈 THIS triggers retry
                }
            }

            val socket = client.newWebSocket(request, listener)

            awaitClose {
                Log.d("Socket", "Closing WebSocket")
                socket.close(1000, null)
            }
        }
            .retryWhen { cause, attempt ->
                Log.d("Socket", "Retrying in 3 seconds... Attempt: $attempt")
                delay(3000)
                true
            }
    }
}