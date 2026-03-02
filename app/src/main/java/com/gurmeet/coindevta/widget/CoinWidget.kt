package com.gurmeet.coindevta.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.currentState
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.gurmeet.coindevta.MainActivity
import org.json.JSONObject

class CoinWidget : GlanceAppWidget() {

    companion object {
        val PricesKey = stringPreferencesKey("prices_json")
        val ConnectionKey = booleanPreferencesKey("connection_state")
    }

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {

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
                context = context,
                prices = prices,
                connected = connected
            )
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        prices: JSONObject,
        connected: Boolean
    ) {

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp)
                .clickable(
                    onClick = actionStartActivity(
                        Intent(context, MainActivity::class.java)
                    )
                )
        ) {

            Text(
                text = "★ Favorite Coins",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(GlanceModifier.height(12.dp))

            if (!connected) {

                Text(
                    text = "Disconnected",
                    style = TextStyle(
                        color = ColorProvider(Color.Red),
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(GlanceModifier.height(10.dp))

              Button(
                    text = "Reload",
                    onClick = actionRunCallback<ReloadAction>()
                )

                return@Column
            }

            if (prices.length() == 0) {

                Text(
                    text = "Waiting for prices...",
                    style = TextStyle(
                        color = ColorProvider(Color.Gray)
                    )
                )

                return@Column
            }

            val entries = prices.keys().asSequence().toList()

            LazyColumn {

                items(entries.size) { index ->

                    val symbol = entries[index]
                    val price = prices.optString(symbol, "--")

                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {

                        Text(
                            text = symbol,
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Spacer(GlanceModifier.defaultWeight())

                        Text(
                            text = price,
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF22C55E)),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(GlanceModifier.height(12.dp))

            Text(
                text = "Live • 2-3 sec refresh",
                style = TextStyle(
                    color = ColorProvider(Color.Gray),
                    fontSize = 10.sp
                )
            )
        }
    }
}