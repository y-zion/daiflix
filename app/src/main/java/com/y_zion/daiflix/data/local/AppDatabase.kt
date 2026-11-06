package com.y_zion.daiflix.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.y_zion.daiflix.data.model.FavoriteEntity
import com.y_zion.daiflix.data.model.SearchHistoryEntity
import com.y_zion.daiflix.data.model.WatchHistoryEntity

@Database(
    entities = [
        WatchHistoryEntity::class,
        SearchHistoryEntity::class,
        FavoriteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vodDao(): VodDao
}
