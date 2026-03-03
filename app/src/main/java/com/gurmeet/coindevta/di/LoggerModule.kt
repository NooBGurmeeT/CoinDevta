package com.gurmeet.coindevta.di

import com.gurmeet.coindevta.logger.AppErrorLogger
import com.gurmeet.coindevta.logger.ErrorLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LoggerModule {

    @Binds
    @Singleton
    abstract fun bindErrorLogger(
        impl: AppErrorLogger
    ): ErrorLogger
}