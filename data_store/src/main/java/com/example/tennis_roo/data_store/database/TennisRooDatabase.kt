package com.example.tennis_roo.data_store.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.tennis_roo.data_store.dao.GameDao
import com.example.tennis_roo.data_store.dao.MatchDao
import com.example.tennis_roo.data_store.dao.PointDao
import com.example.tennis_roo.data_store.entity.GameEntity
import com.example.tennis_roo.data_store.entity.MatchEntity
import com.example.tennis_roo.data_store.entity.PointEntity
import com.example.tennis_roo.data_store.util.DateConverter

/**
 * Room database for the Tennis Roo application.
 */
@Database(
    entities = [MatchEntity::class, GameEntity::class, PointEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(DateConverter::class)
abstract class TennisRooDatabase : RoomDatabase() {
    
    abstract fun matchDao(): MatchDao
    abstract fun gameDao(): GameDao
    abstract fun pointDao(): PointDao
    
    companion object {
        private const val DATABASE_NAME = "tennis_roo_db"
        
        @Volatile
        private var INSTANCE: TennisRooDatabase? = null
        
        fun getInstance(context: Context): TennisRooDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TennisRooDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
