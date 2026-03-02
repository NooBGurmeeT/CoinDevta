package com.gurmeet.coindevta.widget

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore

val Context.widgetDataStore by preferencesDataStore("coin_widget_prefs")

object WidgetKeys {
    val PRICES = stringPreferencesKey("prices_json")
    val CONNECTED = booleanPreferencesKey("socket_connected")
    val FAVORITES = stringSetPreferencesKey("favorites_list")
}