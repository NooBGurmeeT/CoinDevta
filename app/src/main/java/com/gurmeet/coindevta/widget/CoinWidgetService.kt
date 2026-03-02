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
import org.json.JSONObject

@AndroidEntryPoint
class CoinWidgetService : Service() {

    @Inject lateinit var socketManager: BinanceSocketManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val priceState = MutableStateFlow<Map<String, String>>(emptyMap())
    private val connectionState = MutableStateFlow(false)

    private var favoriteCache: Set<String> = emptySet()


    private var lastPushedPricesJson: String = ""
    private var lastConnection: Boolean = false

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())

        serviceScope.launch {
            observeFavorites()
            observeSocket()
            pushLoop()
        }
    }

    // --------------------------------------------------
    // Load favorites before socket starts
    // --------------------------------------------------

    private fun observeFavorites() {
        serviceScope.launch {

            widgetDataStore.data
                .map { it[WidgetKeys.FAVORITES] ?: emptySet() }
                .distinctUntilChanged()
                .collect { newFavorites ->

                    favoriteCache = newFavorites.map { it.uppercase() }.toSet()

                    // Remove prices that are no longer favorite
                    priceState.update { current ->
                        current.filterKeys { favoriteCache.contains(it) }
                    }

                    // Immediately push update to widget
                    lastPushedPricesJson = ""
                }
        }
    }

    // --------------------------------------------------
    // Socket collection
    // --------------------------------------------------

    private fun observeSocket() {
        serviceScope.launch {

            connectionState.value = false

            socketManager.observeAllPrices()
                .onStart { connectionState.value = false }
                .catch { connectionState.value = false }
                .collect { (symbol, price) ->

                    connectionState.value = true

                    val normalized = symbol
                        .substringBefore("@")
                        .uppercase()

                    if (favoriteCache.contains(normalized)) {

                        val formatted = "%.4f".format(price)

                        priceState.update { old ->
                            old.toMutableMap().apply {
                                put(normalized, formatted)
                            }
                        }
                    }
                }
        }
    }

    // --------------------------------------------------
    // Push snapshot every 2.5 seconds
    // --------------------------------------------------

    private fun pushLoop() {
        serviceScope.launch {

            while (isActive) {

                delay(2000)

                val pricesSnapshot = priceState.value
                val connectedSnapshot = connectionState.value

                // Convert price map to JSON string
                val json = JSONObject(pricesSnapshot).toString()

                // Skip if nothing changed (reduces flicker)
                if (json == lastPushedPricesJson &&
                    connectedSnapshot == lastConnection
                ) {
                    continue
                }

                lastPushedPricesJson = json
                lastConnection = connectedSnapshot

                val manager = GlanceAppWidgetManager(this@CoinWidgetService)
                val ids = manager.getGlanceIds(CoinWidget::class.java)

                ids.forEach { glanceId ->

                    updateAppWidgetState(
                        context = this@CoinWidgetService,
                        definition = PreferencesGlanceStateDefinition,
                        glanceId = glanceId
                    ) { prefs ->

                        val mutable = prefs.toMutablePreferences()

                        mutable[CoinWidget.PricesKey] = json
                        mutable[CoinWidget.ConnectionKey] = connectedSnapshot

                        mutable
                    }

                    CoinWidget().update(this@CoinWidgetService, glanceId)
                }
            }
        }
    }

    private suspend fun updateWidget() {
        val manager = GlanceAppWidgetManager(this)
        val ids = manager.getGlanceIds(CoinWidget::class.java)
        ids.forEach { CoinWidget().update(this, it) }
    }

    private fun createNotification(): Notification {

        val channelId = "coin_widget_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Coin Widget",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Live Coin Prices")
            .setContentText("Updating every 2-3 seconds")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}