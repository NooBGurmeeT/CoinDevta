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
import com.gurmeet.coindevta.logger.ErrorLogger
import com.gurmeet.coindevta.logger.LogLevel

/**
 * Handles synchronization between:
 * - Database favorite coins
 * - Widget DataStore
 * - Foreground widget service lifecycle
 */
class WidgetSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observeCoinsUseCase: ObserveCoinsUseCase,
    private val errorLogger: ErrorLogger
) {

    companion object {
        private const val TAG = "WidgetSyncManager"
    }
    /**
     * Reads favorite coins from database,
     * updates widget DataStore,
     * ensures widget service lifecycle is correct,
     * and triggers widget refresh.
     */
    suspend fun syncFavorites() {

        try {

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

        } catch (e: Exception) {
            errorLogger.log(
                TAG,
                "Failed to sync widget favorites",
                LogLevel.ERROR,
                e
            )
        }
    }

    // --------------------------------------------------
    // Ensure foreground service running
    // --------------------------------------------------

    /**
     * Starts CoinWidgetService if not already running.
     */
    private fun ensureServiceRunning() {

        try {
            val intent =
                Intent(context, CoinWidgetService::class.java)

            context.startForegroundService(intent)

        } catch (e: Exception) {
            errorLogger.log(
                TAG,
                "Failed to start widget service",
                LogLevel.ERROR,
                e
            )
        }
    }


    /**
     * Stops widget foreground service when no favorites exist.
     */
    private fun stopService() {

        try {
            val intent =
                Intent(context, CoinWidgetService::class.java)

            context.stopService(intent)

        } catch (e: Exception) {
            errorLogger.log(
                TAG,
                "Failed to stop widget service",
                LogLevel.ERROR,
                e
            )
        }
    }
    /**
     * Forces all widget instances to refresh UI.
     */
    suspend fun refreshWidget() {
        updateWidget()
    }

    /**
     * Updates all active CoinWidget instances safely.
     */
    private suspend fun updateWidget() {

        try {

            val manager =
                GlanceAppWidgetManager(context)

            val ids =
                manager.getGlanceIds(CoinWidget::class.java)

            ids.forEach {
                CoinWidget().update(context, it)
            }

        } catch (e: Exception) {
            errorLogger.log(
                TAG,
                "Failed to update widget UI",
                LogLevel.ERROR,
                e
            )
        }
    }
}