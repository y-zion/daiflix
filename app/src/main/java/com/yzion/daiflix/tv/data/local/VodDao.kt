package com.yzion.daiflix.tv.data.local

import androidx.room.*
import com.yzion.daiflix.tv.data.model.SearchHistoryEntity
import com.yzion.daiflix.tv.data.model.WatchHistoryEntity
import com.yzion.daiflix.tv.data.model.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VodDao {

    // ---- Watch History ----

    @Query("SELECT * FROM watch_history ORDER BY timestamp DESC LIMIT :limit")
    fun getWatchHistory(limit: Int = 50): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE vodId = :vodId")
    suspend fun getWatchHistoryById(vodId: String): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchHistory(entity: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE vodId = :vodId")
    suspend fun deleteWatchHistory(vodId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearWatchHistory()

    // ---- Search History ----

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT :limit")
    fun getSearchHistory(limit: Int = 20): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history WHERE keyword = :keyword LIMIT 1")
    suspend fun getSearchByKeyword(keyword: String): SearchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteSearchHistory(id: Long)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    // ---- Favorites ----

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE vodId = :vodId")
    suspend fun getFavoriteById(vodId: String): FavoriteEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE vodId = :vodId)")
    suspend fun isFavorite(vodId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE vodId = :vodId")
    suspend fun deleteFavorite(vodId: String)
}
