package net.ballmerlabs.subrosa.database

import androidx.lifecycle.LiveData
import androidx.room.*
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.scatterbrain.Post
import net.ballmerlabs.subrosa.scatterbrain.User
import java.util.*

@Dao
interface NewsGroupDao {

    @Transaction
    @Query("SELECT * FROM posts LEFT JOIN user ON user.identity =  posts.author WHERE uuid = (:parent)")
    suspend fun getPostsForGroup(parent: UUID): List<Post>

    @Transaction
    @Query("SELECT * FROM posts LEFT JOIN user ON user.identity = posts.author WHERE uuid = (:parent)")
    fun observePostsForGroup(parent: UUID): LiveData<List<Post>>

    @Query("SELECT * FROM newsgroup WHERE uuid = (:uuid)")
    suspend fun getGroup(uuid: UUID): NewsGroup

    @Query("SELECT * FROM user WHERE identity = :identity")
    suspend fun getUsersByIdentity(identity: UUID): User

    @Query("SELECT * FROM user")
    suspend fun getAllUsers(): List<User>

    @Query("SELECT * FROM user")
    fun observeAllUsers(): LiveData<List<User>>

    @Query("SELECT * FROM user WHERE owned = :owned")
    suspend fun getAllOwnedUsers(owned: Boolean): List<User>

    @Query("SELECT * FROM user WHERE owned = :owned")
    fun observeAllOwnedUsers(owned: Boolean): LiveData<List<User>>

    @Transaction
    @Query("SELECT * FROM newsgroup WHERE uuid = (:uuid)")
    suspend fun getGroupWithChildren(uuid: UUID): NewsGroupChildren

    @Transaction
    @Query("SELECT * FROM newsgroup WHERE uuid = (:uuid)")
    fun observeGroupWithChildren(uuid: UUID): LiveData<NewsGroupChildren>

    @Transaction
    @Query("SELECT * FROM newsgroup WHERE parent IS NULL")
    fun observeAllGroups(): LiveData<List<NewsGroup>>

    @Transaction
    @Query("SELECT * FROM newsgroup WHERE parent IS NULL AND groupName LIKE '%' || :name || '%'")
    fun observeAllGroups(name: String): LiveData<List<NewsGroup>>

    @Query("DELETE FROM user WHERE identity = :identity")
    suspend fun deleteByIdentity(identity: UUID): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroup(newsGroup: NewsGroup)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPost(vararg post: Post)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroups(newsGroup: NewsGroup)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroups(newsGroup: List<NewsGroup>)

    @Insert
    suspend fun insertUsers(vararg user: User)

    @Delete
    suspend fun deleteUsers(vararg user: User)

    @Insert
    suspend fun insertUsers(user: List<User>)
}