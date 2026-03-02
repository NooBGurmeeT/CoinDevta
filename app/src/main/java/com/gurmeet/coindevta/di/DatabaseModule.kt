package com.gurmeet.coindevta.di

import android.content.Context
import androidx.room.Room
import com.gurmeet.coindevta.data.local.database.AppDatabase
import com.gurmeet.coindevta.data.local.dao.CoinDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val DATABASE_NAME = "coin_db"

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideAnimeDao(
        database: AppDatabase
    ): CoinDao {
        return database.coinDao()
    }
}