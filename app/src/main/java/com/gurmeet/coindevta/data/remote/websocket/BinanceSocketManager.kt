package com.gurmeet.coindevta.data.remote.websocket

import com.gurmeet.coindevta.domain.model.TickerUpdate
import com.gurmeet.coindevta.logger.ErrorLogger
import com.gurmeet.coindevta.logger.LogLevel
import com.gurmeet.coindevta.util.NetworkMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import okhttp3.*
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maintains a single shared WebSocket connection to Binance mini ticker stream.
 * Emits real-time ticker updates as a hot SharedFlow across the app.
 */
@Singleton
class BinanceSocketManager @Inject constructor(
    private val client: OkHttpClient,
    private val errorLogger: ErrorLogger,
    private val networkMonitor: NetworkMonitor
) {

    companion object {
        private const val TAG = "BinanceSocketManager"
    }

    // IO scope used to keep socket alive independently
    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Shared hot flow so entire app uses one WebSocket connection
    private val sharedTickerFlow: SharedFlow<TickerUpdate> by lazy {
        createSocketFlow()
            .buffer(capacity = 5000) // Prevents backpressure blocking
            .shareIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                replay = 0
            )
    }

    /**
     * Exposes real-time ticker updates.
     */
    fun observeAllPrices(): Flow<TickerUpdate> = sharedTickerFlow

    /**
     * Creates WebSocket flow with automatic retry on failure.
     */
    private fun createSocketFlow(): Flow<TickerUpdate> {

        return callbackFlow {

            errorLogger.log(
                tag = TAG,
                message = "Creating WebSocket connection",
                level = LogLevel.DEBUG
            )

            val request = Request.Builder()
                .url("wss://stream.binance.com:9443/ws/!miniTicker@arr")
                .build()

            val listener = object : WebSocketListener() {

                // Called when socket connects successfully
                override fun onOpen(
                    webSocket: WebSocket,
                    response: Response
                ) {
                    errorLogger.log(
                        tag = TAG,
                        message = "WebSocket connected",
                        level = LogLevel.INFO
                    )
                }

                // Receives ticker array and emits each update
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
                        errorLogger.log(
                            tag = TAG,
                            message = "WebSocket parse error",
                            level = LogLevel.ERROR,
                            throwable = e
                        )
                    }
                }

                // Triggered on network/server failure
                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?
                ) {
                    errorLogger.log(
                        tag = TAG,
                        message = "WebSocket failure",
                        level = LogLevel.CRITICAL,
                        throwable = t
                    )
                    close(t)
                }

                // Triggered when socket closes normally
                override fun onClosed(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String
                ) {
                    errorLogger.log(
                        tag = TAG,
                        message = "WebSocket closed. Code: $code, Reason: $reason",
                        level = LogLevel.WARNING
                    )
                    close()
                }
            }

            val socket = client.newWebSocket(request, listener)

            // Ensures socket closes when flow is cancelled
            awaitClose {
                errorLogger.log(
                    tag = TAG,
                    message = "Closing WebSocket",
                    level = LogLevel.DEBUG
                )
                socket.close(1000, null)
            }
        }
            // Retries connection after delay when failure occurs
            .retryWhen { cause, attempt ->

                errorLogger.log(
                    tag = TAG,
                    message = "WebSocket failure. Attempt: $attempt",
                    level = LogLevel.WARNING,
                    throwable = cause
                )

                // If no internet → wait until it becomes available
                if (!networkMonitor.isConnected.value) {
                    networkMonitor.isConnected
                        .filter { it }
                        .first()
                }

                // Always apply delay (even if internet is already available)
                val delayMillis = 5000L
                delay(delayMillis)

                errorLogger.log(
                    tag = TAG,
                    message = "Retrying WebSocket after ${delayMillis}ms",
                    level = LogLevel.INFO
                )

                true
            }
    }
}