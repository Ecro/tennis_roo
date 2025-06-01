package com.example.tennis_roo.data_store.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tennis_roo.data_store.entity.PointEntity
import com.example.tennis_roo.data_store.entity.Player
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the points table.
 */
@Dao
interface PointDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: PointEntity): Long
    
    @Update
    suspend fun updatePoint(point: PointEntity)
    
    @Delete
    suspend fun deletePoint(point: PointEntity)
    
    @Query("SELECT * FROM points WHERE gameId = :gameId ORDER BY timestamp ASC")
    fun getPointsForGame(gameId: Long): Flow<List<PointEntity>>
    
    @Query("SELECT * FROM points WHERE id = :pointId")
    fun getPointById(pointId: Long): Flow<PointEntity?>
    
    @Query("SELECT * FROM points WHERE gameId = :gameId ORDER BY timestamp DESC LIMIT 1")
    fun getLastPointForGame(gameId: Long): Flow<PointEntity?>
    
    @Query("SELECT COUNT(*) FROM points WHERE gameId = :gameId AND winner = :player")
    fun getPointCountForPlayerInGame(gameId: Long, player: Player): Flow<Int>
    
    @Query("DELETE FROM points WHERE gameId = :gameId")
    suspend fun deletePointsForGame(gameId: Long)
    
    @Query("DELETE FROM points WHERE id = (SELECT MAX(id) FROM points WHERE gameId = :gameId)")
    suspend fun deleteLastPointForGame(gameId: Long)
    
    @Query("""
        SELECT p.* FROM points p
        INNER JOIN games g ON p.gameId = g.id
        WHERE g.matchId = :matchId
        ORDER BY p.timestamp ASC
    """)
    fun getAllPointsForMatch(matchId: Long): Flow<List<PointEntity>>
}
