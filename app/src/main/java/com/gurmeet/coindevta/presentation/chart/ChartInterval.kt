package com.gurmeet.coindevta.presentation.chart

enum class ChartInterval(val apiValue: String, val limit: Int) {
    HOUR("1m", 60),        // last 60 minutes
    DAY("5m", 288),        // 24 hours (5m candles)
    MONTH("1h", 720)       // 30 days (1h candles)
}