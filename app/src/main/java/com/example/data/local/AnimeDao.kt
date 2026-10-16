package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Anime
import com.example.data.model.Episode
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDao {
    @Query("SELECT * FROM anime ORDER BY title COLLATE NOCASE ASC")
    fun getAllAnime(): Flow<List<Anime>>

    @Query("SELECT * FROM anime WHERE id = :id")
    fun getAnimeById(id: String): Flow<Anime?>

    @Query("SELECT * FROM anime WHERE id = :id")
    suspend fun getAnimeByIdOnce(id: String): Anime?

    @Query("SELECT * FROM anime WHERE malId = :malId AND malId != '' AND malId != '0' LIMIT 1")
    suspend fun getAnimeByMalIdOnce(malId: String): Anime?

    @Query("SELECT * FROM anime WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getFavorites(): Flow<List<Anime>>

    @Query("SELECT * FROM anime WHERE isInWatchlist = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getWatchlist(): Flow<List<Anime>>

    @Query("SELECT * FROM anime WHERE watchStatus = :status AND isInWatchlist = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getAnimeByWatchStatus(status: String): Flow<List<Anime>>

    @Query("UPDATE anime SET watchStatus = :status, isCompleted = :isCompleted, isInWatchlist = :inWatchlist, lastUpdated = :updatedAt WHERE id = :id")
    suspend fun updateWatchStatusFull(
        id: String,
        status: String,
        isCompleted: Boolean,
        inWatchlist: Boolean = true,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE anime SET watchStatus = :status WHERE id = :id")
    suspend fun updateWatchStatus(id: String, status: String)

    @Query("UPDATE anime SET isInWatchlist = 0 WHERE isInWatchlist = 1")
    suspend fun clearWatchlist()

    @Query("SELECT * FROM anime WHERE isCompleted = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getCompleted(): Flow<List<Anime>>

    @Query("SELECT * FROM anime WHERE scheduleDay = :day")
    fun getAnimeByScheduleDay(day: String): Flow<List<Anime>>

    @Query("SELECT * FROM anime WHERE title LIKE '%' || :query || '%' OR nativeTitle LIKE '%' || :query || '%' OR genres LIKE '%' || :query || '%'")
    fun searchAnime(query: String): Flow<List<Anime>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(animeList: List<Anime>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(anime: Anime)

    @Update
    suspend fun update(anime: Anime)

    @Query("UPDATE anime SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE anime SET isInWatchlist = :inWatchlist WHERE id = :id")
    suspend fun updateWatchlist(id: String, inWatchlist: Boolean)

    @Query("UPDATE anime SET isCompleted = :completed WHERE id = :id")
    suspend fun updateCompleted(id: String, completed: Boolean)

    @Query("SELECT * FROM episodes WHERE animeId = :animeId ORDER BY number ASC")
    fun getEpisodesForAnime(animeId: String): Flow<List<Episode>>

    @Query("SELECT * FROM episodes WHERE animeId = :animeId ORDER BY number ASC")
    suspend fun getEpisodesForAnimeSync(animeId: String): List<Episode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<Episode>)

    @Query("DELETE FROM episodes WHERE animeId = :animeId")
    suspend fun deleteEpisodesForAnime(animeId: String)

    @Query("DELETE FROM episodes WHERE animeId = :animeId AND number > :maxEpisode")
    suspend fun deleteEpisodesAboveNumber(animeId: String, maxEpisode: Int)

    @Query("SELECT COUNT(*) FROM anime")
    suspend fun getAnimeCount(): Int
}
