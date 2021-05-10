package net.ballmerlabs.subrosa.database

import androidx.room.Dao
import androidx.room.Query
import java.util.*

@Dao
interface NewsGroupDao {
    @Query("SELECT * FROM posts WHERE parent = (:parent)")
    fun getPostsForGroup(parent: UUID): List<Post>

    @Query("SELECT * FROM newsgroup WHERE uuid = (:uuid)")
    fun getGroup(uuid: UUID): NewsGroup
}