package com.gurmeet.coindevta.analytics

/**
 * Central analytics constants used across ViewModel and UI layer.
 */
object AnalyticsConstants {

    object Chart {

        const val SCREEN_OPENED = "chart_screen_opened"
        const val INTERVAL_SELECTED = "chart_interval_selected"
        const val BACK_CLICKED = "chart_back_clicked"

        const val PARAM_SYMBOL = "symbol"
        const val PARAM_INTERVAL = "interval"
    }

    object Home {

        const val SCREEN_OPENED = "home_screen_opened"
        const val FAVORITE_TOGGLED = "favorite_toggled"
        const val COIN_PINNED = "coin_pinned"
        const val COIN_UNPINNED = "coin_unpinned"
        const val COIN_CLICKED = "coin_clicked"

        const val PARAM_SYMBOL = "symbol"
    }

    object Main {

        const val ACTIVITY_OPENED = "main_activity_opened"
    }
}