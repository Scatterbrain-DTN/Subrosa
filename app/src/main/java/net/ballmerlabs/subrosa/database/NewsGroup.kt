package net.ballmerlabs.subrosa.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "newsgroup",
    foreignKeys = [
        ForeignKey(
            entity = NewsGroup::class,
            parentColumns = ["uuid"],
            childColumns = ["parent"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NewsGroup(
    @PrimaryKey val uuid: UUID,
    val humanReadable: String,
    val owner: UUID?,
    val sig: ByteArray?,
    @ColumnInfo(name = "parent") val parent: UUID
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NewsGroup

        if (uuid != other.uuid) return false
        if (humanReadable != other.humanReadable) return false
        if (owner != other.owner) return false
        if (sig != null) {
            if (other.sig == null) return false
            if (!sig.contentEquals(other.sig)) return false
        } else if (other.sig != null) return false
        if (parent != other.parent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + humanReadable.hashCode()
        result = 31 * result + (owner?.hashCode() ?: 0)
        result = 31 * result + (sig?.contentHashCode() ?: 0)
        result = 31 * result + parent.hashCode()
        return result
    }
}