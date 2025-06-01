package com.example.tennis_roo.data_store.util

import androidx.room.TypeConverter
import java.util.Date

/**
 * Type converter for Room to convert between Date and Long.
 */
class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
