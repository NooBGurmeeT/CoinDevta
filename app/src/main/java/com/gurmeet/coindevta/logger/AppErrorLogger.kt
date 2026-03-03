package com.gurmeet.coindevta.logger

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppErrorLogger @Inject constructor() : ErrorLogger {

    override fun log(
        tag: String,
        message: String,
        level: LogLevel,
        throwable: Throwable?
    ) {

        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARNING -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
            LogLevel.CRITICAL -> Log.wtf(tag, message, throwable)
        }

        // Later you can forward to Crashlytics here
    }
}