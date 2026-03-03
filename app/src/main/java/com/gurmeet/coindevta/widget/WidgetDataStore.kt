package com.gurmeet.coindevta.widget

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore

val Context.widgetDataStore by preferencesDataStore("coin_widget_prefs")

object WidgetKeys {
    val FAVORITES = stringSetPreferencesKey("favorites_list")
}