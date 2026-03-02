package com.gurmeet.coindevta.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.glance.appwidget.GlanceAppWidgetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import com.gurmeet.coindevta.domain.usecase.ObserveCoinsUseCase

class WidgetSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observeCoinsUseCase: ObserveCoinsUseCase
) {

    // --------------------------------------------------
    // Sync favorites from DB to DataStore
    // --------------------------------------------------

    suspend fun syncFavorites() {

        val favorites = observeCoinsUseCase()
            .first()
            .filter { it.isFavorite }
            .map { it.symbol.uppercase() }
            .toSet()

        context.widgetDataStore.edit {
            it[WidgetKeys.FAVORITES] = favorites
        }

        if (favorites.isNotEmpty()) {
            ensureServiceRunning()
        } else {
            stopService()
        }

        updateWidget()
    }

    // --------------------------------------------------
    // Ensure foreground service running
    // --------------------------------------------------

    private fun ensureServiceRunning() {
        val intent = Intent(context, CoinWidgetService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    // --------------------------------------------------
    // Stop service if no favorites
    // --------------------------------------------------

    private fun stopService() {
        val intent = Intent(context, CoinWidgetService::class.java)
        context.stopService(intent)
    }

    // --------------------------------------------------
    // Manual refresh (used by app if needed)
    // --------------------------------------------------

    suspend fun refreshWidget() {
        updateWidget()
    }

    // --------------------------------------------------
    // Update all widget instances
    // --------------------------------------------------

    private suspend fun updateWidget() {
        val manager = GlanceAppWidgetManager(context)
        val ids = manager.getGlanceIds(CoinWidget::class.java)
        ids.forEach { CoinWidget().update(context, it) }
    }
}