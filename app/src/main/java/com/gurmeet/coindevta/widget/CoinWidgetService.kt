package com.gurmeet.coindevta.widget

import android.app.*
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import com.gurmeet.coindevta.data.remote.websocket.BinanceSocketManager
import com.gurmeet.coindevta.domain.model.TickerUpdate
import org.json.JSONObject

@AndroidEntryPoint
class CoinWidgetService : Service() {

    @Inject lateinit var socketManager: BinanceSocketManager

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    data class WidgetPrice(
        val price: String,
        val positive: Boolean
    )

    private val priceState =
        MutableStateFlow<Map<String, WidgetPrice>>(emptyMap())

    private val connectionState =
        MutableStateFlow(false)

    private var favoriteCache: Set<String> = emptySet()

    private var lastPushedPricesJson: String = ""
    private var lastConnection: Boolean = false

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())

        observeFavorites()
        observeSocket()
        pushLoop()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }
    // --------------------------------------------------
    // FAVORITES OBSERVER
    // --------------------------------------------------

    private fun observeFavorites() {
        serviceScope.launch {

            widgetDataStore.data
                .map { it[WidgetKeys.FAVORITES] ?: emptySet() }
                .distinctUntilChanged()
                .collect { newFavorites ->

                    favoriteCache =
                        newFavorites.map { it.uppercase() }.toSet()

                    priceState.update { current ->
                        current.filterKeys { favoriteCache.contains(it) }
                    }

                    lastPushedPricesJson = ""
                }
        }
    }

    private fun observeSocket() {
        serviceScope.launch {

            socketManager.observeAllPrices()
                .collect { update: TickerUpdate ->

                    connectionState.value = true

                    val symbol =
                        update.symbol
                            .substringBefore("@")
                            .uppercase()

                    if (favoriteCache.contains(symbol)) {

                        priceState.update { old ->
                            old.toMutableMap().apply {
                                put(
                                    symbol,
                                    WidgetPrice(
                                        price = "%.4f".format(update.currentPrice),
                                        positive = update.isPositive24h
                                    )
                                )
                            }
                        }
                    }
                }
        }
    }

    // --------------------------------------------------
    // PUSH LOOP (SAFE & STABLE)
    // --------------------------------------------------

    private fun pushLoop() {
        serviceScope.launch {

            while (isActive) {

                delay(5000)

                if (favoriteCache.isEmpty()) continue

                val pricesSnapshot = priceState.value
                val connectedSnapshot = connectionState.value

                val wrapper = JSONObject()

                pricesSnapshot.forEach { (key, value) ->
                    val obj = JSONObject().apply {
                        put("price", value.price)
                        put("positive", value.positive)
                    }
                    wrapper.put(key, obj)
                }

                val json = wrapper.toString()

                if (json == lastPushedPricesJson &&
                    connectedSnapshot == lastConnection
                ) continue

                lastPushedPricesJson = json
                lastConnection = connectedSnapshot

                val manager =
                    GlanceAppWidgetManager(this@CoinWidgetService)

                val ids =
                    manager.getGlanceIds(CoinWidget::class.java)

                ids.forEach { glanceId ->

                    updateAppWidgetState(
                        context = this@CoinWidgetService,
                        definition = PreferencesGlanceStateDefinition,
                        glanceId = glanceId
                    ) { prefs ->

                        val mutable =
                            prefs.toMutablePreferences()

                        mutable[CoinWidget.PricesKey] = json
                        mutable[CoinWidget.ConnectionKey] =
                            connectedSnapshot

                        mutable
                    }

                    CoinWidget().update(
                        this@CoinWidgetService,
                        glanceId
                    )
                }
            }
        }
    }

    // --------------------------------------------------
    // FOREGROUND NOTIFICATION
    // --------------------------------------------------

    private fun createNotification(): Notification {

        val channelId = "coin_widget_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "Coin Widget",
                NotificationManager.IMPORTANCE_MIN
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Live Coin Prices")
            .setContentText("Updating every 5-6 seconds")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setForegroundServiceBehavior(
                NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
            )
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}