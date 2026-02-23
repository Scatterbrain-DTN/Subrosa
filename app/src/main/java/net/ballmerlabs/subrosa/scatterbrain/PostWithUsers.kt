package net.ballmerlabs.subrosa.scatterbrain

import androidx.room.Embedded
import androidx.room.Relation

data class PostWithUsers (
    @Embedded
    var post: Post,
    @Relation(parentColumn = "author", entityColumn = "identity", entity = User::class)
    var user: User? = null
)