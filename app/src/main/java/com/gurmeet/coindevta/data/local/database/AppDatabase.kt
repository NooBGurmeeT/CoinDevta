package com.gurmeet.coindevta.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gurmeet.coindevta.data.local.dao.CoinDao
import com.gurmeet.coindevta.data.local.entity.CoinEntity


@Database(
    entities = [CoinEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase: RoomDatabase(){
    abstract fun coinDao(): CoinDao
}
