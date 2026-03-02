package com.gurmeet.coindevta.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class CoinWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = CoinWidget()
}