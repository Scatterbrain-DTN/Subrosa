package net.ballmerlabs.subrosa.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "posts",
    foreignKeys = [
        ForeignKey(
            entity = NewsGroup::class, 
            parentColumns = ["uuid"], 
            childColumns = ["parent"], 
            onDelete = ForeignKey.CASCADE
        )
])
data class Post(
    val owner: UUID,
    val body: String,
    @ColumnInfo(name = "parent") val parent: UUID
) {
    @PrimaryKey(autoGenerate = true) var id = 0
}