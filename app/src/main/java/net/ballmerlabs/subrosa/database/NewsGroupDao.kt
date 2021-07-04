package net.ballmerlabs.subrosa.database

import androidx.room.*
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.scatterbrain.Post
import java.util.*

@Dao
interface NewsGroupDao {
    @Query("SELECT * FROM posts WHERE parent = (:parent)")
    suspend fun getPostsForGroup(parent: UUID): List<Post>

    @Query("SELECT * FROM newsgroup WHERE uuid = (:uuid)")
    suspend fun getGroup(uuid: UUID): NewsGroup

    @Query("SELECT * FROM user WHERE identity = :identity")
    suspend fun getUsersByIdentity(identity: UUID): User

    @Query("SELECT * FROM user")
    suspend fun getAllUsers(): List<User>

    @Transaction
    @Query("SELECT * FROM newsgroup WHERE uuid = (:uuid)")
    suspend fun getGroupWithChildren(uuid: UUID): NewsGroupChildren

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroup(newsGroup: NewsGroup)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPost(post: Post)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroups(newsGroup: NewsGroup)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroups(newsGroup: List<NewsGroup>)

    @Insert
    suspend fun insertUsers(vararg user: User)

    @Insert
    suspend fun insertUsers(user: List<User>)
}