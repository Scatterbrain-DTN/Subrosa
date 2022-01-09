package net.ballmerlabs.subrosa

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.lelloman.identicon.drawable.GithubIdenticonDrawable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import net.ballmerlabs.scatterbrainsdk.ScatterMessage
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import net.ballmerlabs.subrosa.database.NewsGroupChildren
import net.ballmerlabs.subrosa.database.NewsGroupDao
import net.ballmerlabs.subrosa.database.User
import net.ballmerlabs.subrosa.scatterbrain.*
import net.ballmerlabs.subrosa.util.uuidConvert
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Named
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class NewsRepository @Inject constructor(
    @ApplicationContext val context: Context,
    val dao: NewsGroupDao,
    val sdkComponent: ScatterbrainApi,
    @Named(ScatterbrainModule.API_COROUTINE_SCOPE) val coroutineScope: CoroutineScope
    ) {

    private val prefs = context.getSharedPreferences(SubrosaApplication.SHARED_PREFS_DEFAULT, Context.MODE_PRIVATE)

    private val connectedCallback: MutableSet<(connected: Boolean) -> Unit> = HashSet()
    private var isConnected = false
    private val refreshInProgress = AtomicReference(false)

    init {
        sdkComponent.broadcastReceiver.register()
    }

    suspend fun tryBind() = withTimeout(1000) {
        sdkComponent.binderWrapper.getScatterMessages("")
    }

    suspend fun isConnected(): Boolean {
        val c = sdkComponent.binderWrapper.isConnected()
        updateConnected(c)
        return c
    }

    private fun updateConnected(connected: Boolean) {
        if (isConnected != connected ) {
            connectedCallback.forEach { v ->
                v(connected)
            }
            isConnected = connected
        }
    }

    private suspend fun updateConnected() {
        val c = sdkComponent.binderWrapper.isConnected()
        updateConnected(c)
    }

    @ExperimentalCoroutinesApi
    suspend fun observeConnections() = callbackFlow {
        val callback: (connected: Boolean) -> Unit = { b ->
            offer(b)
        }
        connectedCallback.add(callback)

        awaitClose { connectedCallback.remove(callback) }
    }

    suspend fun requireConnected() {
        if (!isConnected()) {
            throw IllegalStateException("routingService not connected")
        }
    }

    private fun postToArray(parent: NewsGroup, user: UUID, header: String, body: String): ByteArray {
        return parent.hash + uuidConvert(user) + header.encodeToByteArray() + body.encodeToByteArray()
    }

    suspend fun sendPost(parent: NewsGroup, user: UUID, header: String, body: String) {
        requireConnected()

        val identity = sdkComponent.binderWrapper.getIdentity(user)
            ?: throw IllegalStateException("user does not exist")

        Log.v("debug", "send post got identity ${identity.fingerprint}")
        val post = Post(
            parent,
            user,
            header,
            body,
            sdkComponent.binderWrapper.sign(identity, postToArray(
                parent,
                user,
                header,
                body
            ))
        )

        Log.v("debug", "send post signed post")
        val message = ScatterMessage.Builder.newInstance(post.bytes)
            .setApplication(context.getString(R.string.scatterbrainapplication))
            .build()
        var par = post.parent
        val groupMsgs = ArrayList<ScatterMessage>()
        while (par.hasParent) {
            yield()
            par = dao.getGroup(par.parentCol!!)
            val groupMsg = ScatterMessage.Builder.newInstance(post.bytes)
                .setApplication(context.getString(R.string.scatterbrainapplication))
                .build()
            groupMsgs.add(groupMsg)
        }
        Log.v("debug", "send post sent newsgroups")
        dao.insertPost(post)
        Log.v("debug", "send post inserted post")
        if (isConnected()) {
            sdkComponent.binderWrapper.sendMessage(groupMsgs)
            sdkComponent.binderWrapper.sendMessage(message, post.author)
        }
    }

    suspend fun createUser(name: String, bio: String, imageBitmap: Bitmap? = null) : User = withContext(
        Dispatchers.IO
    ) {
        requireConnected()
        val hashcode = (name + bio).hashCode()
        val image = imageBitmap?: GithubIdenticonDrawable(64, 64, hashcode).toBitmap()
        val id = sdkComponent.binderWrapper.generateIdentity(name)
        val user = User(
            identity = id.fingerprint,
            name = name,
            bio = bio,
            owned = true
        )
        user.writeImage(image, context)
        dao.insertUsers(user)
        user
    }

    suspend fun insertUser(user: User, image: Bitmap) {
        updateConnected()
        user.writeImage(image, context)
        dao.insertUsers(user)
    }

    suspend fun readAllUsers(): List<User> {
        updateConnected()
        return dao.getAllUsers()
    }


    suspend fun getUser(uuid: UUID): User {
        return dao.getUsersByIdentity(uuid)
    }


    private fun getUsersWithFiles(userlist: List<User>): LiveData<List<User>> = liveData {
        userlist.forEach { user ->
            user.getImageFromPath(context)
            Log.v(TAG, "getting image for ${user.identity}")
        }
        Log.v(TAG, "emit")
        emit(userlist)
    }

    fun observeUsers(owned: Boolean? = null): LiveData<List<User>> {
        return if(owned == null) {
            dao.observeAllUsers()
                .switchMap { userlist ->
                    Log.v(TAG, "fnmef")
                    getUsersWithFiles(userlist)
                  }
        } else {
            dao.observeAllOwnedUsers(owned)
                .switchMap { userlist -> getUsersWithFiles(userlist) }
        }
    }

    fun observePosts(group: NewsGroup): LiveData<List<Post>> {
        return dao.observePostsForGroup(group.uuid)
    }

    fun observePosts(group: UUID): LiveData<List<Post>> {
        return dao.observePostsForGroup(group)
    }

    suspend fun getPosts(group: UUID): List<Post> {
        return dao.getPostsForGroup(group)
    }

    suspend fun getPosts(newsGroup: NewsGroup): List<Post> {
        return dao.getPostsForGroup(newsGroup.uuid)
    }

    suspend fun readUsers(owned: Boolean): List<User> {
        updateConnected()

        return dao.getAllOwnedUsers(owned)
            .map { u ->
                u.getImageFromPath(context)
                u
            }
    }

    suspend fun readUsers(uuid: UUID): User {
        updateConnected()
        val user = dao.getUsersByIdentity(uuid)
        user.getImageFromPath(context)
        return user
    }

    suspend fun insertGroup(group: NewsGroup) {
        updateConnected()
        val message = ScatterMessage.Builder.newInstance(group.bytes)
            .setApplication(context.getString(R.string.scatterbrainapplication))
            .build()
        dao.insertGroup(group)
        if (isConnected()) {
            sdkComponent.binderWrapper.sendMessage(message)
        }
    }


    suspend fun insertGroup(group: List<NewsGroup>) {
        updateConnected()
        dao.insertGroups(group)
        val messages = group.map { g ->
            ScatterMessage.Builder.newInstance(g.bytes)
                .setApplication(context.getString(R.string.scatterbrainapplication))
                .build()
        }
        if (isConnected()) {
            sdkComponent.binderWrapper.sendMessage(messages)
        }
    }


    suspend fun deleteUser(user: UUID): Boolean {
        return dao.deleteByIdentity(user) == 1
    }

    suspend fun createGroup(name: String, parent: NewsGroup): NewsGroup {
        updateConnected()
        val uuid = UUID.randomUUID()
        Log.e("debug", "parent emptu: ${parent.empty}")
        val group = NewsGroup(
            uuid = uuid,
            parentCol = if (parent.empty) null else parent.uuid,
            name = name,
            parentHash = if (parent.empty) null else parent.hash
        )
        insertGroup(group)
        return group
    }

    suspend fun getChildren(group: UUID): List<NewsGroup> {
        updateConnected()
        val dbchild: NewsGroupChildren?= dao.getGroupWithChildren(group)
        return dbchild?.children?: ArrayList()
    }


    fun observeChildren(group: UUID): LiveData<List<NewsGroup>> {
        return dao.observeGroupWithChildren(group)
            .map { children -> children.children }
    }

    suspend fun observePosts(): Flow<Post> = flow {
        requireConnected()
        sdkComponent.binderWrapper.observeMessages(context.getString(R.string.scatterbrainapplication))
            .onEach { currentCoroutineContext().ensureActive() }
            .map { messages ->
                for (message in messages) {
                    if (!message.isFile) {
                        val type = Message.parse(message.body!!)
                        when (type.typeVal) {
                            TypeVal.POST -> {
                                val post = Message.parse<Post>(message.body!!, type)
                                dao.insertPost(post)
                                emit(post)
                            }
                            TypeVal.NEWSGROUP -> {
                                val newsGroup = Message.parse<NewsGroup>(message.body!!, type)
                                dao.insertGroup(newsGroup)
                            }
                            else -> {
                                yield()
                            }
                        }
                    }
                }
            }
    }

    private suspend fun processScatterMessages(messages: List<ScatterMessage>) {
        messages.forEach { message ->
            try {
                if (!message.isFile) {
                    val type = Message.parse(message.body!!)
                    Log.v(TAG, "processing message type ${type.typeVal}")
                    when (type.typeVal) {
                        TypeVal.POST -> {
                            val post = Message.parse<Post>(message.body!!, type)
                            dao.insertPost(post)
                        }
                        TypeVal.NEWSGROUP -> {
                            val newsgroup = Message.parse<NewsGroup>(message.body!!, type)
                            dao.insertGroup(newsgroup)
                        }
                        else -> {
                            Log.e(TAG, "invalid message type received")
                        }
                    }
                }
            } catch (exc: Exception) {
                Log.e(TAG, "failed to process message from ${message.id}: $exc")
                exc.printStackTrace()
            }
        }
    }

    fun getLastSyncTime(): Date {
        val time = prefs.getLong(PREF_LAST_SYNC_TIME, 0)
        return Date(time)
    }

    fun setLastSyncTime(time: Date) {
        prefs.edit {
            putLong(PREF_LAST_SYNC_TIME, time.time)
            commit()
        }
    }

    suspend fun fullSync(): Boolean {
        requireConnected()
        if (refreshInProgress.getAndSet(true)) {
            return false
        }
        val messages = withContext(Dispatchers.IO) { sdkComponent.binderWrapper.getScatterMessages(context.getString(R.string.scatterbrainapplication)) }
        withContext(Dispatchers.Default) { processScatterMessages(messages) }
        refreshInProgress.set(false)
        return true
    }

    suspend fun fullSync(since: Date): Boolean {
        requireConnected()
        if (refreshInProgress.getAndSet(true)) {
            return false
        }
        val messages = withContext(Dispatchers.IO) {
            sdkComponent.binderWrapper.getScatterMessages(context.getString(R.string.scatterbrainapplication), since)
        }
        withContext(Dispatchers.Default) {
            processScatterMessages(messages)
        }
        refreshInProgress.set(false)
        return true
    }

    suspend fun fullSync(start: Date, end: Date): Boolean {
        requireConnected()
        if (refreshInProgress.getAndSet(true)) {
            return false
        }
        val messages = withContext(Dispatchers.IO) {
            sdkComponent.binderWrapper.getScatterMessages(context.getString(R.string.scatterbrainapplication), start, end)
        }
        withContext(Dispatchers.Default) { processScatterMessages(messages) }
        refreshInProgress.set(false)
        return true
    }

    companion object {
        const val TAG = "NewsRepository"
        const val PREF_LAST_SYNC_TIME  = "last_sync_time"
    }

}