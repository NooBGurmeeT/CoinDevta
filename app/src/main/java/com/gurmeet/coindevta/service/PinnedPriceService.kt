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
import kotlin.math.absoluteValue
import androidx.core.graphics.toColorInt

@AndroidEntryPoint
class PinnedPriceService : Service() {

    @Inject lateinit var socketManager: BinanceSocketManager
    @Inject lateinit var observeCoinsUseCase: ObserveCoinsUseCase

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var socketJob: Job? = null
    private var currentPinned: String? = null

    companion object {
        private const val CHANNEL_ID = "pinned_coin_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(
            NOTIFICATION_ID,
            baseNotification("Waiting for pinned coin...")
        )
        observePinnedCoin()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    // --------------------------------------------------
    // Observe pinned coin
    // --------------------------------------------------

    private fun observePinnedCoin() {

        serviceScope.launch {

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
        }
    }

    // --------------------------------------------------
    // Socket
    // --------------------------------------------------

    private fun restartSocket(symbol: String) {

        stopSocket()

        socketJob = serviceScope.launch {

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
        }
    }

    private fun stopSocket() {
        socketJob?.cancel()
        socketJob = null
    }

    // --------------------------------------------------
    // Notification UI
    // --------------------------------------------------

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

    private fun baseNotification(text: String): Notification {

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pinned Coin")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

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
            "%.2f".format(changePercent.absoluteValue)

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
    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}