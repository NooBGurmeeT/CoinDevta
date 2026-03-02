package com.gurmeet.coindevta.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import com.gurmeet.coindevta.data.remote.websocket.BinanceSocketManager
import com.gurmeet.coindevta.domain.usecase.ObserveCoinsUseCase

@AndroidEntryPoint
class PinnedPriceService : Service() {

    @Inject lateinit var socketManager: BinanceSocketManager
    @Inject lateinit var observeCoinsUseCase: ObserveCoinsUseCase

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var socketJob: Job? = null
    private var currentPinned: String? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1001, baseNotification("Waiting for pinned coin..."))
        observePinnedCoin()
    }

    // -----------------------------------
    // Observe pinned coin continuously
    // -----------------------------------

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

    // -----------------------------------
    // Restart socket safely
    // -----------------------------------

    private fun restartSocket(symbol: String) {

        stopSocket()

        socketJob = serviceScope.launch {

            socketManager.observeAllPrices()
                .collect { (socketSymbol, price) ->

                    if (socketSymbol.equals(symbol, true)) {
                        updateNotification(symbol, price)
                    }
                }
        }
    }

    private fun stopSocket() {
        socketJob?.cancel()
        socketJob = null
    }

    // -----------------------------------
    // Notification
    // -----------------------------------

    private fun baseNotification(text: String): Notification {

        val channelId = "pinned_coin_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pinned Coin",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Pinned Coin")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(symbol: String, price: Double) {

        val notification =
            baseNotification("$symbol  $price")

        val manager =
            getSystemService(NotificationManager::class.java)

        manager.notify(1001, notification)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}