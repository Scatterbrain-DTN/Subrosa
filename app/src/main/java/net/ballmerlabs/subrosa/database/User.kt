package net.ballmerlabs.subrosa.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class User(
    @PrimaryKey val identity: UUID,
    val name: String,
    val bio: String,
    val imagePath: String
){}