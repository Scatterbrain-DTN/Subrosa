package net.ballmerlabs.subrosa.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.*


class UuidTypeConverter {
    @TypeConverter
    fun uuidToString(uuid: UUID): String {
        return uuid.toString()
    }
    
    @TypeConverter
    fun stringToUUID(string: String): UUID {
        return UUID.fromString(string)
    }
}

@Database(entities = [NewsGroup::class, Post::class], version = 1)
@TypeConverters(UuidTypeConverter::class)
abstract class RoomDatabase : RoomDatabase() {
    abstract fun newsGroupDao(): NewsGroupDao
}