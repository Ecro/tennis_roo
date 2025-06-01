package com.example.tennis_roo.data_store.repository

import android.content.Context
import com.example.tennis_roo.data_store.database.TennisRooDatabase
import com.example.tennis_roo.data_store.entity.GameEntity
import com.example.tennis_roo.data_store.entity.MatchEntity
import com.example.tennis_roo.data_store.entity.MatchFormat
import com.example.tennis_roo.data_store.entity.Player
import com.example.tennis_roo.data_store.entity.PointEntity
import com.example.tennis_roo.data_store.entity.StrokeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repository for accessing match data.
 * This class provides a clean API for the rest of the application to interact with the data layer.
 */
class MatchRepository(context: Context) {
    
    private val database = TennisRooDatabase.getInstance(context)
    private val matchDao = database.matchDao()
    private val gameDao = database.gameDao()
    private val pointDao = database.pointDao()
    
    // Match operations
    
    /**
     * Creates a new match with the given parameters.
     *
     * @param player1Name The name of player 1
     * @param player2Name The name of player 2
     * @param format The match format
     * @return The ID of the created match
     */
    suspend fun createMatch(
        player1Name: String,
        player2Name: String,
        format: MatchFormat
    ): Long {
        val match = MatchEntity(
            player1Name = player1Name,
            player2Name = player2Name,
            startTime = System.currentTimeMillis(),
            format = format,
            completed = false
        )
        return matchDao.insertMatch(match)
    }
    
    /**
     * Gets all matches.
     *
     * @return A flow of all matches
     */
    fun getAllMatches(): Flow<List<MatchEntity>> {
        return matchDao.getAllMatches()
    }
    
    /**
     * Gets a match by ID.
     *
     * @param matchId The ID of the match
     * @return A flow of the match
     */
    fun getMatch(matchId: Long): Flow<MatchEntity?> {
        return matchDao.getMatchById(matchId)
    }
    
    /**
     * Gets the current (active) match.
     *
     * @return A flow of the current match, or null if there is no active match
     */
    fun getCurrentMatch(): Flow<MatchEntity?> {
        return matchDao.getCurrentMatch()
    }
    
    /**
     * Completes a match.
     *
     * @param matchId The ID of the match to complete
     */
    suspend fun completeMatch(matchId: Long) {
        matchDao.completeMatch(matchId, System.currentTimeMillis())
    }
    
    /**
     * Deletes a match and all associated games and points.
     *
     * @param matchId The ID of the match to delete
     */
    suspend fun deleteMatch(matchId: Long) {
        matchDao.deleteMatchById(matchId)
    }
    
    // Game operations
    
    /**
     * Creates a new game for a match.
     *
     * @param matchId The ID of the match
     * @param server The player who is serving
     * @param tieBreak Whether this is a tie break game
     * @return The ID of the created game
     */
    suspend fun createGame(
        matchId: Long,
        server: Player,
        tieBreak: Boolean = false
    ): Long {
        val gameCount = gameDao.getGameCountForMatch(matchId).first()
        val game = GameEntity(
            matchId = matchId,
            gameNumber = gameCount + 1,
            server = server,
            tieBreak = tieBreak
        )
        return gameDao.insertGame(game)
    }
    
    /**
     * Gets all games for a match.
     *
     * @param matchId The ID of the match
     * @return A flow of all games for the match
     */
    fun getGamesForMatch(matchId: Long): Flow<List<GameEntity>> {
        return gameDao.getGamesForMatch(matchId)
    }
    
    /**
     * Gets the current game for a match.
     *
     * @param matchId The ID of the match
     * @return A flow of the current game, or null if there are no games
     */
    fun getCurrentGameForMatch(matchId: Long): Flow<GameEntity?> {
        return gameDao.getCurrentGameForMatch(matchId)
    }
    
    // Point operations
    
    /**
     * Records a point for a game.
     *
     * @param gameId The ID of the game
     * @param winner The player who won the point
     * @param strokeType The type of stroke that won the point
     * @param confidence The confidence level of the stroke detection
     * @return The ID of the created point
     */
    suspend fun recordPoint(
        gameId: Long,
        winner: Player,
        strokeType: StrokeType,
        confidence: Float
    ): Long {
        val point = PointEntity(
            gameId = gameId,
            timestamp = System.currentTimeMillis(),
            winner = winner,
            strokeType = strokeType,
            confidence = confidence
        )
        return pointDao.insertPoint(point)
    }
    
    /**
     * Gets all points for a game.
     *
     * @param gameId The ID of the game
     * @return A flow of all points for the game
     */
    fun getPointsForGame(gameId: Long): Flow<List<PointEntity>> {
        return pointDao.getPointsForGame(gameId)
    }
    
    /**
     * Gets the point count for a player in a game.
     *
     * @param gameId The ID of the game
     * @param player The player
     * @return A flow of the point count
     */
    fun getPointCountForPlayer(gameId: Long, player: Player): Flow<Int> {
        return pointDao.getPointCountForPlayerInGame(gameId, player)
    }
    
    /**
     * Undoes the last point for a game.
     *
     * @param gameId The ID of the game
     */
    suspend fun undoLastPoint(gameId: Long) {
        pointDao.deleteLastPointForGame(gameId)
    }
    
    /**
     * Gets all points for a match.
     *
     * @param matchId The ID of the match
     * @return A flow of all points for the match
     */
    fun getAllPointsForMatch(matchId: Long): Flow<List<PointEntity>> {
        return pointDao.getAllPointsForMatch(matchId)
    }
    
    /**
     * Exports a match as a string.
     *
     * @param matchId The ID of the match
     * @return A flow of the match export string
     */
    fun exportMatch(matchId: Long): Flow<String> {
        return matchDao.getMatchById(matchId).map { match ->
            if (match == null) {
                return@map "Match not found"
            }
            
            val games = gameDao.getGamesForMatch(matchId).first()
            val points = pointDao.getAllPointsForMatch(matchId).first()
            
            buildString {
                append("Tennis Roo Match Export\n")
                append("=======================\n\n")
                
                append("Match: ${match.player1Name} vs ${match.player2Name}\n")
                append("Format: ${match.format}\n")
                append("Start Time: ${java.util.Date(match.startTime)}\n")
                match.endTime?.let { append("End Time: ${java.util.Date(it)}\n") }
                append("Status: ${if (match.completed) "Completed" else "In Progress"}\n\n")
                
                append("Games: ${games.size}\n")
                games.forEach { game ->
                    append("Game ${game.gameNumber}: Server=${game.server}")
                    if (game.tieBreak) append(" (Tie Break)")
                    append("\n")
                    
                    val gamePoints = points.filter { it.gameId == game.id }
                    val player1Points = gamePoints.count { it.winner == Player.PLAYER_1 }
                    val player2Points = gamePoints.count { it.winner == Player.PLAYER_2 }
                    append("  Score: $player1Points-$player2Points\n")
                    
                    gamePoints.forEach { point ->
                        append("  Point: Winner=${point.winner}, Stroke=${point.strokeType}, ")
                        append("Confidence=${point.confidence}, ")
                        append("Time=${java.util.Date(point.timestamp)}\n")
                    }
                    append("\n")
                }
            }
        }
    }
}
