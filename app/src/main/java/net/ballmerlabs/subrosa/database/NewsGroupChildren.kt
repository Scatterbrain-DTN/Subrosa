package net.ballmerlabs.subrosa.database

import androidx.room.Embedded
import androidx.room.Relation

data class NewsGroupChildren(
    @Embedded val newsGroup: NewsGroup,
    @Relation(
        parentColumn = "uuid",
        entityColumn = "parent"
    )
    val children: List<NewsGroup>
)