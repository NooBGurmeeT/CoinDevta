package com.gurmeet.coindevta.presentation.chart

enum class ChartInterval(
    val apiValue: String,
    val limit: Int,
    val updateMillis: Long
) {

    HOUR(
        apiValue = "1m",
        limit = 180,
        updateMillis = 20_000L // update every  20sec
    ),

    DAY(
        apiValue = "5m",
        limit = 288,
        updateMillis = 60_000L // append every 1 min for smoother effect
    ),

    MONTH(
        apiValue = "1h",
        limit = 720,
        updateMillis = 300_000L // update every 5 minutes
    )
}

data class ChartPoint(
    val time: Long,
    val price: Double
)