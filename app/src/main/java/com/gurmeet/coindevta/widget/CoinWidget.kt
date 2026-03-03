package com.gurmeet.coindevta.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.*
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.gurmeet.coindevta.MainActivity
import org.json.JSONObject

/**
 * Glance AppWidget displaying favorite coin prices.
 *
 * Reads price data from DataStore preferences and renders
 * live updates with connection state handling.
 */
class CoinWidget : GlanceAppWidget() {

    companion object {

        private const val TAG = "CoinWidget"

        // JSON string storing symbol -> price mapping
        val PricesKey = stringPreferencesKey("prices_json")

        // Indicates whether WebSocket connection is active
        val ConnectionKey = booleanPreferencesKey("connection_state")
    }

    override val stateDefinition = PreferencesGlanceStateDefinition

    /**
     * Entry point for rendering widget content.
     * Reads stored preferences and builds UI.
     */
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {

        provideContent {

            val prefs = currentState<Preferences>()

            val json = prefs[PricesKey] ?: "{}"
            val connected = prefs[ConnectionKey] ?: false

            val prices = try {
                JSONObject(json)
            } catch (e: Exception) {
                JSONObject("{}")
            }

            WidgetContent(
                prices = prices,
                connected = connected
            )
        }
    }

    /**
     * Main widget UI layout.
     *
     * @param prices JSON object containing symbol-price pairs
     * @param connected Whether WebSocket connection is active
     */
    @Composable
    private fun WidgetContent(
        prices: JSONObject,
        connected: Boolean
    ) {

        // ✅ WHOLE WIDGET CLICKABLE CONTAINER
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF0F172A)))
                .clickable(actionStartActivity<MainActivity>())
        ) {

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                Text(
                    text = "★ Favorite Coins",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFF8FAFC)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(GlanceModifier.height(12.dp))

                // --------------------------------------------------
                // NO INTERNET / DISCONNECTED STATE
                // --------------------------------------------------

                if (!connected) {

                    Text(
                        text = "No Internet",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFEF4444)),
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(GlanceModifier.height(10.dp))

                    // ✅ Button overrides parent click
                    Button(
                        text = "Reload",
                        onClick = actionRunCallback<ReloadAction>()
                    )

                    return@Column
                }

                // Shows loading state
                if (prices.length() == 0) {

                    Text(
                        text = "Waiting for prices...",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF94A3B8))
                        )
                    )

                    return@Column
                }

                val entries = prices.keys().asSequence().toList()

                LazyColumn(
                    modifier = GlanceModifier.defaultWeight()
                ) {

                    items(entries.size) { index ->

                        val symbol = entries[index]
                        val obj = prices.optJSONObject(symbol)

                        val price =
                            obj?.optString("price", "--") ?: "--"

                        val positive =
                            obj?.optBoolean("positive", true) ?: true

                        val priceColor =
                            if (positive)
                                ColorProvider(Color(0xFF22C55E))
                            else
                                ColorProvider(Color(0xFFEF4444))

                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(
                                    ColorProvider(Color(0xFF1E293B))
                                )
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 10.dp
                                )
                        ) {

                            Text(
                                text = symbol,
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFE2E8F0)),
                                    fontWeight = FontWeight.Medium
                                )
                            )

                            Spacer(GlanceModifier.defaultWeight())

                            Text(
                                text = price,
                                style = TextStyle(
                                    color = priceColor,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(GlanceModifier.height(8.dp))
                    }
                }

                Spacer(GlanceModifier.height(12.dp))

                Text(
                    text = "Live • 5-6 sec refresh",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF94A3B8)),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}