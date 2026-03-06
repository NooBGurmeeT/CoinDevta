package com.gurmeet.coindevta.service

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import com.gurmeet.coindevta.data.remote.websocket.BinanceSocketManager
import com.gurmeet.coindevta.domain.model.TickerUpdate
import com.gurmeet.coindevta.domain.usecase.ObserveCoinsUseCase
import com.gurmeet.coindevta.logger.ErrorLogger
import com.gurmeet.coindevta.logger.LogLevel
import com.gurmeet.coindevta.util.NetworkMonitor
import kotlin.math.absoluteValue

/**
 * Foreground service responsible for showing
 * real-time notification updates of the currently pinned coin.
 *
 * It listens to database changes for pinned coin,
 * restarts WebSocket stream accordingly,
 * and updates notification with live prices.
 */
@AndroidEntryPoint
class PinnedPriceService : Service() {

    @Inject lateinit var socketManager: BinanceSocketManager
    @Inject lateinit var observeCoinsUseCase: ObserveCoinsUseCase
    @Inject lateinit var errorLogger: ErrorLogger
    @Inject lateinit var networkMonitor: NetworkMonitor

    companion object {
        private const val TAG = "PinnedPriceService"
        private const val CHANNEL_ID = "pinned_coin_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var socketJob: Job? = null
    private var currentPinned: String? = null

    /**
     * Called when service is created.
     * Initializes notification channel and starts observing pinned coin.
     */
    override fun onCreate() {
        super.onCreate()

        createChannel()

        startForeground(
            NOTIFICATION_ID,
            baseNotification("Waiting for pinned coin...")
        )

        observeNetwork()
        observePinnedCoin()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int = START_STICKY

    // --------------------------------------------------
    // Network awareness
    // --------------------------------------------------

    /**
     * Observes network connectivity and handles socket accordingly.
     */
    private fun observeNetwork() {

        serviceScope.launch {

            networkMonitor.isConnected
                .collect { connected ->

                    if (!connected) {

                        errorLogger.log(
                            TAG,
                            "Internet lost",
                            LogLevel.WARNING
                        )

                        stopSocket()

                        updateOfflineNotification()

                    } else {

                        errorLogger.log(
                            TAG,
                            "Internet restored",
                            LogLevel.INFO
                        )

                        currentPinned?.let {
                            restartSocket(it)
                        }
                    }
                }
        }
    }

    /**
     * Updates notification when internet is lost.
     */
    private fun updateOfflineNotification() {

        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentPinned ?: "Pinned Coin")
                .setContentText("No Internet Connection")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .build()

        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    // --------------------------------------------------
    // Observe pinned coin
    // --------------------------------------------------

    /**
     * Observes database for pinned coin changes.
     * Restarts WebSocket stream when pinned symbol changes.
     */
    private fun observePinnedCoin() {

        serviceScope.launch {

            try {
                observeCoinsUseCase()
                    .map { coins ->
                        coins.firstOrNull { it.isPinned }?.symbol
                    }
                    .distinctUntilChanged()
                    .collect { newPinned ->

                        if (newPinned == null) {
                            stopSocket()
                            stopSelf()
                            return@collect
                        }

                        if (newPinned != currentPinned) {
                            currentPinned = newPinned
                            restartSocket(newPinned)
                        }
                    }
            } catch (e: Exception) {
                errorLogger.log(
                    TAG,
                    "Error observing pinned coin",
                    LogLevel.ERROR,
                    e
                )
            }
        }
    }

    // --------------------------------------------------
    // WebSocket handling
    // --------------------------------------------------

    /**
     * Restarts WebSocket collection for the specified symbol.
     *
     * @param symbol The currently pinned coin symbol.
     */
    private fun restartSocket(symbol: String) {

        stopSocket()

        socketJob = serviceScope.launch {

            try {
                socketManager.observeAllPrices()
                    .collect { update: TickerUpdate ->

                        if (update.symbol.equals(symbol, true)) {

                            updateNotification(
                                symbol = symbol,
                                price = update.currentPrice,
                                openPrice = update.openPrice24h,
                                isPositive = update.isPositive24h
                            )
                        }
                    }
            } catch (e: Exception) {
                errorLogger.log(
                    TAG,
                    "WebSocket collection failed",
                    LogLevel.ERROR,
                    e
                )
            }
        }
    }

    /**
     * Stops current WebSocket job safely.
     */
    private fun stopSocket() {
        socketJob?.cancel()
        socketJob = null
    }

    // --------------------------------------------------
    // Notification
    // --------------------------------------------------

    /**
     * Creates notification channel for pinned coin updates.
     */
    private fun createChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pinned Coin",
                NotificationManager.IMPORTANCE_LOW
            )

            channel.description = "Shows live updates for pinned coin"
            channel.enableLights(false)
            channel.enableVibration(false)

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    /**
     * Base notification shown before price updates start.
     */
    private fun baseNotification(text: String): Notification {

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pinned Coin")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    /**
     * Updates foreground notification with live price data.
     *
     * @param symbol Coin symbol
     * @param price Current live price
     * @param openPrice 24h open price
     * @param isPositive Whether price is positive compared to 24h open
     */
    private fun updateNotification(
        symbol: String,
        price: Double,
        openPrice: Double,
        isPositive: Boolean
    ) {

        val changePercent =
            ((price - openPrice) / openPrice) * 100

        val formattedPrice = "%.4f".format(price)
        val formattedPercent =
            "%.4f".format(changePercent.absoluteValue)

        val arrow = if (isPositive) "▲" else "▼"

        val subtitle =
            "$arrow $formattedPercent% (24h)"

        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(symbol)
                .setContentText("$formattedPrice  •  $subtitle")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "$symbol\n" +
                                    "$formattedPrice\n" +
                                    "$subtitle"
                        )
                )
                .setSmallIcon(
                    if (isPositive)
                        android.R.drawable.arrow_up_float
                    else
                        android.R.drawable.arrow_down_float
                )
                .setColor(
                    if (isPositive)
                        Color.parseColor("#22C55E")
                    else
                        Color.parseColor("#EF4444")
                )
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .build()

        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    /**
     * Cleans up coroutine scope when service is destroyed.
     */
    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}