package com.yzion.daiflix.tv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yzion.daiflix.tv.data.model.FavoriteEntity
import com.yzion.daiflix.tv.data.model.SearchHistoryEntity
import com.yzion.daiflix.tv.data.model.WatchHistoryEntity

@Database(
    entities = [
        WatchHistoryEntity::class,
        SearchHistoryEntity::class,
        FavoriteEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vodDao(): VodDao
}

/**
 * v1 stored the 1-based episode number and did not record which play source was used.
 * v2 stores the 0-based list index plus the source index so resume can reopen the
 * exact episode even when a title has multiple sources.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE watch_history ADD COLUMN sourceIndex INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "UPDATE watch_history SET episodeIndex = CASE WHEN episodeIndex > 0 THEN episodeIndex - 1 ELSE 0 END"
        )
    }
}
