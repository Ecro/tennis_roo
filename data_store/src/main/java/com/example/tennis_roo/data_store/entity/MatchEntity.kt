package com.example.tennis_roo.data_store.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.tennis_roo.data_store.util.DateConverter

/**
 * Entity representing a tennis match.
 */
@Entity(tableName = "matches")
@TypeConverters(DateConverter::class)
data class MatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val player1Name: String,
    val player2Name: String,
    val startTime: Long,
    val endTime: Long? = null,
    val format: MatchFormat,
    val completed: Boolean = false
)

/**
 * Enum representing the different match formats.
 */
enum class MatchFormat {
    BEST_OF_3_SETS,
    PRO_SET_FIRST_TO_8
}

/**
 * Entity representing a game within a match.
 */
@Entity(
    tableName = "games",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("matchId")]
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val matchId: Long,
    val gameNumber: Int,
    val server: Player,
    val tieBreak: Boolean = false
)

/**
 * Entity representing a point within a game.
 */
@Entity(
    tableName = "points",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("gameId")]
)
data class PointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val timestamp: Long,
    val winner: Player,
    val strokeType: StrokeType,
    val confidence: Float
)

/**
 * Enum representing the players in a match.
 */
enum class Player {
    PLAYER_1,
    PLAYER_2
}

/**
 * Enum representing the types of tennis strokes.
 */
enum class StrokeType {
    SERVE,
    FOREHAND,
    BACKHAND,
    VOLLEY,
    SMASH,
    UNKNOWN
}
