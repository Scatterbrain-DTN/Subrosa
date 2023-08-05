package net.ballmerlabs.subrosa.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.scatterbrain.Post
import net.ballmerlabs.subrosa.scatterbrain.User
import java.util.*


class UuidTypeConverter {
    @TypeConverter
    fun uuidToString(uuid: UUID?): String? {
        return uuid?.toString()
    }
    
    @TypeConverter
    fun stringToUUID(string: String?): UUID? {
        return if (string != null) UUID.fromString(string) else null
    }
}

@Database(
    entities = [
        NewsGroup::class,
        Post::class,
        User::class
               ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
@TypeConverters(UuidTypeConverter::class)
abstract class RoomDatabase : RoomDatabase() {
    abstract fun newsGroupDao(): NewsGroupDao
}