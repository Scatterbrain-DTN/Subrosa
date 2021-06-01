package net.ballmerlabs.subrosa.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.scatterbrain.Post
import java.util.*

@Dao
interface NewsGroupDao {
    @Query("SELECT * FROM posts WHERE parent = (:parent)")
    suspend fun getPostsForGroup(parent: UUID): List<Post>

    @Query("SELECT * FROM newsgroup WHERE uuid = (:uuid)")
    suspend fun getGroup(uuid: UUID): NewsGroup

    @Transaction
    @Query("SELECT * FROM newsgroup WHERE uuid = (:uuid)")
    suspend fun getGroupWithChildren(uuid: UUID): NewsGroupChildren

    @Insert
    suspend fun insertGroup(newsGroup: NewsGroup)


    @Insert
    suspend fun insertPost(post: Post)

    @Insert
    suspend fun insertGroups(vararg newsGroup: NewsGroup)
}