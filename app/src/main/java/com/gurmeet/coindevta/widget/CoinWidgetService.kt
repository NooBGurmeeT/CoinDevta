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
import com.gurmeet.coindevta.logger.ErrorLogger
import com.gurmeet.coindevta.logger.LogLevel
import com.gurmeet.coindevta.util.NetworkMonitor
import org.json.JSONObject

/**
 * Foreground service responsible for:
 * - Listening to WebSocket live prices
 * - Filtering favorite coins
 * - Updating Glance widget state safely
 * - Monitoring connection health
 */
@AndroidEntryPoint
class CoinWidgetService : Service() {

    @Inject lateinit var socketManager: BinanceSocketManager
    @Inject lateinit var errorLogger: ErrorLogger
    @Inject lateinit var networkMonitor: NetworkMonitor

    companion object {
        private const val TAG = "CoinWidgetService"
        private const val CHANNEL_ID = "coin_widget_channel"
        private const val NOTIFICATION_ID = 1
    }

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Represents UI-safe widget price model.
     */
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

    /**
     * Initializes foreground service and starts observers.
     */
    override fun onCreate() {
        super.onCreate()

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        observeNetwork() // ✅ ADDED
        observeFavorites()
        observeSocket()
        pushLoop()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int = START_STICKY

    /**
     * Observes network connectivity and updates widget connection state.
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
                        connectionState.value = false
                    } else {
                        errorLogger.log(
                            TAG,
                            "Internet restored",
                            LogLevel.INFO
                        )
                    }
                }
        }
    }

    /**
     * Observes DataStore for favorite coins changes.
     * Filters cached prices when favorites change.
     */
    private fun observeFavorites() {
        serviceScope.launch {
            try {
                widgetDataStore.data
                    .map { it[WidgetKeys.FAVORITES] ?: emptySet() }
                    .distinctUntilChanged()
                    .collect { newFavorites ->

                        favoriteCache =
                            newFavorites.map { it.uppercase() }.toSet()

                        priceState.update { current ->
                            current.filterKeys {
                                favoriteCache.contains(it)
                            }
                        }

                        lastPushedPricesJson = ""
                    }
            } catch (e: Exception) {
                errorLogger.log(
                    TAG,
                    "Error observing favorites",
                    LogLevel.ERROR,
                    e
                )
            }
        }
    }

    /**
     * Collects live WebSocket updates and maintains connection state.
     */
    private fun observeSocket() {

        // WebSocket collector
        serviceScope.launch {
            try {
                socketManager.observeAllPrices()
                    .onStart {
                        connectionState.value = false
                    }
                    .catch {
                        connectionState.value = false

                        errorLogger.log(
                            TAG,
                            "Socket error in widget service",
                            LogLevel.ERROR,
                            it
                        )
                    }
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
            } catch (e: Exception) {
                errorLogger.log(
                    TAG,
                    "WebSocket collection failed",
                    LogLevel.ERROR,
                    e
                )
            }
        }

        // Connection health monitor
        serviceScope.launch {

            var lastUpdateTime = System.currentTimeMillis()

            launch {
                priceState.collect {
                    lastUpdateTime = System.currentTimeMillis()
                }
            }

            while (isActive) {
                delay(15000)

                val now = System.currentTimeMillis()

                if (now - lastUpdateTime > 15000) {
                    connectionState.value = false
                }
            }
        }
    }

    /**
     * Pushes price updates to Glance widget every 5 seconds
     * only when data or connection state changes.
     */
    private fun pushLoop() {
        serviceScope.launch {

            while (isActive) {

                delay(5000)

                if (favoriteCache.isEmpty()) continue

                // Force offline state if no internet
                if (!networkMonitor.isConnected.value) {
                    connectionState.value = false
                }

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

                try {
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

                } catch (e: Exception) {
                    errorLogger.log(
                        TAG,
                        "Failed to push widget update",
                        LogLevel.ERROR,
                        e
                    )
                }
            }
        }
    }

    // --------------------------------------------------
    // FOREGROUND NOTIFICATION
    // --------------------------------------------------

    /**
     * Creates foreground notification required for background execution.
     */
    private fun createNotification(): Notification {

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Coin Widget",
            NotificationManager.IMPORTANCE_MIN
        )

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Live Coin Prices")
            .setContentText("Updating every 5-6 seconds")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setForegroundServiceBehavior(
                NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
            )
            .build()
    }

    /**
     * Cancels coroutine scope when service is destroyed.
     */
    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}