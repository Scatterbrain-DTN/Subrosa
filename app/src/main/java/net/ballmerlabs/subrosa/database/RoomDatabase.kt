package net.ballmerlabs.subrosa.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
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
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5, spec = net.ballmerlabs.subrosa.database.RoomDatabase.MigrationSpecIdentity::class)
    ]
)
@TypeConverters(UuidTypeConverter::class)
abstract class RoomDatabase : RoomDatabase() {
    abstract fun newsGroupDao(): NewsGroupDao

    @DeleteColumn(
        tableName = "posts",
        columnName = "identity"
    )
    @DeleteColumn(
        tableName = "posts",
        columnName = "userName"
    )
    @DeleteColumn(
        tableName = "posts",
        columnName = "bio"
    )
    @DeleteColumn(
        tableName = "posts",
        columnName = "owned"
    )
    @DeleteColumn(
        tableName = "posts",
        columnName = "imageBytes"
    )
    class MigrationSpecIdentity: AutoMigrationSpec
}