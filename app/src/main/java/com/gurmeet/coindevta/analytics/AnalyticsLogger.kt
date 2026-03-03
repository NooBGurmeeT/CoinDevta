package com.gurmeet.coindevta.analytics

interface AnalyticsLogger {
    fun track(event: AnalyticsEvent)
}