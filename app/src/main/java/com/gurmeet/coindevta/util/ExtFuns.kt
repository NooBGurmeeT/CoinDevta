package com.gurmeet.coindevta.util

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun <T> Flow<T>.windowedByTime(
    windowMillis: Long
): Flow<List<T>> = channelFlow {

    val buffer = mutableListOf<T>()

    launch {
        collect { value ->
            buffer.add(value)
        }
    }

    launch {
        while (true) {
            delay(windowMillis)
            if (buffer.isNotEmpty()) {
                val snapshot = buffer.toList()
                buffer.clear()
                send(snapshot)
            }
        }
    }
}