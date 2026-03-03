package com.gurmeet.coindevta.analytics

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAnalyticsLogger @Inject constructor() : AnalyticsLogger {

    override fun track(event: AnalyticsEvent) {

        Log.d(
            "ANALYTICS",
            "Event: ${event.name} | Params: ${event.params}"
        )

        // Later connect to Firebase / Mixpanel here
    }
}