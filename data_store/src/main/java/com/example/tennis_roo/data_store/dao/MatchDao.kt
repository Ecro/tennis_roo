package com.example.tennis_roo.data_store.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.tennis_roo.data_store.entity.MatchEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the matches table.
 */
@Dao
interface MatchDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long
    
    @Update
    suspend fun updateMatch(match: MatchEntity)
    
    @Delete
    suspend fun deleteMatch(match: MatchEntity)
    
    @Query("SELECT * FROM matches ORDER BY startTime DESC")
    fun getAllMatches(): Flow<List<MatchEntity>>
    
    @Query("SELECT * FROM matches WHERE id = :matchId")
    fun getMatchById(matchId: Long): Flow<MatchEntity?>
    
    @Query("SELECT * FROM matches WHERE completed = 0 ORDER BY startTime DESC LIMIT 1")
    fun getCurrentMatch(): Flow<MatchEntity?>
    
    @Query("UPDATE matches SET completed = 1, endTime = :endTime WHERE id = :matchId")
    suspend fun completeMatch(matchId: Long, endTime: Long)
    
    @Query("DELETE FROM matches WHERE id = :matchId")
    suspend fun deleteMatchById(matchId: Long)
}
