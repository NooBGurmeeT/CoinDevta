package com.gurmeet.coindevta.logger

interface ErrorLogger {

    fun log(
        tag: String,
        message: String,
        level: LogLevel = LogLevel.ERROR,
        throwable: Throwable? = null
    )
}