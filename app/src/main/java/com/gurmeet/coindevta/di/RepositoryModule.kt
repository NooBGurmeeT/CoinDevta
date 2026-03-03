package com.gurmeet.coindevta.di

import com.gurmeet.coindevta.data.local.dao.CoinDao
import com.gurmeet.coindevta.data.remote.api.BinanceApi
import com.gurmeet.coindevta.data.remote.websocket.BinanceSocketManager
import com.gurmeet.coindevta.data.repository.ChartRepositoryImpl
import com.gurmeet.coindevta.data.repository.CoinRepositoryImpl
import com.gurmeet.coindevta.domain.repository.ChartRepository
import com.gurmeet.coindevta.domain.repository.CoinRepository
import com.gurmeet.coindevta.logger.ErrorLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideCoinRepository(
        api: BinanceApi,
        dao: CoinDao,
        socketManager: BinanceSocketManager,
        errorLogger: ErrorLogger
    ): CoinRepository {
        return CoinRepositoryImpl(
            api,
            dao,
            socketManager,
            errorLogger
        )
    }

    @Provides
    @Singleton
    fun provideChartRepository(
        api: BinanceApi,
        errorLogger: ErrorLogger,
    ): ChartRepository {
        return ChartRepositoryImpl(
            api,
            errorLogger
        )
    }
}