package com.gurmeet.coindevta.util


sealed class Response<out T> {
    data class Success<T>(val data: T) : Response<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Response<Nothing>()
    object Loading : Response<Nothing>()
}