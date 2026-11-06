package com.yzion.daiflix.tv.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey
    val vodId: String,
    val title: String,
    val coverUrl: String,
    val episodeIndex: Int,
    @ColumnInfo(defaultValue = "0")
    val sourceIndex: Int = 0,
    val position: Long = 0,
    val duration: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val keyword: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val vodId: String,
    val title: String,
    val coverUrl: String,
    val category: String,
    val addedAt: Long = System.currentTimeMillis()
)
