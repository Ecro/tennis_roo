package com.example.tennis_roo.data_store.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tennis_roo.data_store.entity.GameEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the games table.
 */
@Dao
interface GameDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity): Long
    
    @Update
    suspend fun updateGame(game: GameEntity)
    
    @Delete
    suspend fun deleteGame(game: GameEntity)
    
    @Query("SELECT * FROM games WHERE matchId = :matchId ORDER BY gameNumber ASC")
    fun getGamesForMatch(matchId: Long): Flow<List<GameEntity>>
    
    @Query("SELECT * FROM games WHERE id = :gameId")
    fun getGameById(gameId: Long): Flow<GameEntity?>
    
    @Query("SELECT * FROM games WHERE matchId = :matchId ORDER BY gameNumber DESC LIMIT 1")
    fun getCurrentGameForMatch(matchId: Long): Flow<GameEntity?>
    
    @Query("SELECT COUNT(*) FROM games WHERE matchId = :matchId")
    fun getGameCountForMatch(matchId: Long): Flow<Int>
    
    @Query("DELETE FROM games WHERE matchId = :matchId")
    suspend fun deleteGamesForMatch(matchId: Long)
}
