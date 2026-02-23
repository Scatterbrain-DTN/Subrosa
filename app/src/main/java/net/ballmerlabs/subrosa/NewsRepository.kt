package net.ballmerlabs.subrosa

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.ScatterMessage
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import net.ballmerlabs.subrosa.database.NewsGroupChildren
import net.ballmerlabs.subrosa.database.NewsGroupDao
import net.ballmerlabs.subrosa.scatterbrain.Message
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.scatterbrain.Post
import net.ballmerlabs.subrosa.scatterbrain.PostWithUsers
import net.ballmerlabs.subrosa.scatterbrain.ScatterbrainModule
import net.ballmerlabs.subrosa.scatterbrain.TypeVal
import net.ballmerlabs.subrosa.scatterbrain.User
import net.ballmerlabs.subrosa.util.srLog
import net.ballmerlabs.subrosa.util.uuidConvert
import java.util.Date
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Named

class NewsRepository @Inject constructor(
    @param:ApplicationContext val context: Context,
    val dao: NewsGroupDao,
    val sdkComponent: ScatterbrainApi,
    @param:Named(ScatterbrainModule.API_COROUTINE_SCOPE) val coroutineScope: CoroutineScope
) {

    private val log by srLog()

    private val prefs =
        context.getSharedPreferences(SubrosaApplication.SHARED_PREFS_DEFAULT, Context.MODE_PRIVATE)

    private val connectedCallback: MutableSet<(connected: Boolean) -> Unit> = HashSet()
    private var isConnected = false
    private val refreshInProgress = AtomicReference(false)

    init {
        sdkComponent.broadcastReceiver.register()
    }

    suspend fun isConnected(): Boolean {
        sdkComponent.binderWrapper.bindService()
        val c = sdkComponent.binderWrapper.isConnected()
        updateConnected(c)
        return c
    }

    private fun updateConnected(connected: Boolean) {
        if (isConnected != connected) {
            connectedCallback.forEach { v ->
                v(connected)
            }
            isConnected = connected
        }
    }

    suspend fun requireConnected() {
        if (!isConnected()) {
            throw IllegalStateException("routingService not connected")
        }
    }

    private fun postToArray(
        parent: NewsGroup,
        user: UUID,
        header: String,
        body: String
    ): ByteArray {
        return parent.hash + uuidConvert(user) + header.encodeToByteArray() + body.encodeToByteArray()
    }


    suspend fun sbSendPost(post: Post) {
        val message = ScatterMessage.Builder.newInstance(context, post.bytes)
            .setApplication(context.getString(R.string.scatterbrainapplication))
            .build()
        var par = post.parent
        val groupMsgs = mutableListOf(
            ScatterMessage.Builder.newInstance(context, post.bytes)
                .setApplication(context.getString(R.string.scatterbrainapplication))
                .build()
        )
        while (par.hasParent) {
            yield()
            par = dao.getGroup(par.parentCol!!)
            val groupMsg = ScatterMessage.Builder.newInstance(context, post.bytes)
                .setApplication(context.getString(R.string.scatterbrainapplication))
                .build()
            groupMsgs.add(groupMsg)
        }

        if (isConnected()) {
            sdkComponent.binderWrapper.sendMessage(groupMsgs)
            val author = post.author
            if (author != null) {
                sdkComponent.binderWrapper.sendMessage(message, author)
            } else {
                sdkComponent.binderWrapper.sendMessage(message)
            }
        }
    }


    suspend fun sbSendGroup(group: NewsGroup) {
        val message = ScatterMessage.Builder.newInstance(context, group.bytes)
            .setApplication(context.getString(R.string.scatterbrainapplication))
            .build()
        dao.insertGroup(group)
        if (isConnected()) {
            sdkComponent.binderWrapper.sendMessage(message)
        }
    }

    suspend fun sendPost(parent: NewsGroup, user: UUID?, header: String, body: String) {
        requireConnected()

        val identity = if (user != null) {
            sdkComponent.binderWrapper.getIdentity(user)
                ?: throw IllegalStateException("user does not exist")
        } else {
            null
        }
        val sig = if (identity != null && user != null) {
            sdkComponent.binderWrapper.sign(
                identity.fingerprint, postToArray(
                    parent,
                    user,
                    header,
                    body
                )
            )
        } else {
            null
        }

        log.v("send post got identity ${identity?.fingerprint}")
        val post = Post(
            parent = parent,
            author = user,
            header = header,
            body = body,
            sig = sig
        )

        log.v("send post signed post")
        sbSendPost(post)
        log.v("send post sent newsgroups")
        dao.insertPost(post)
    }

    suspend fun countPost(newsGroup: UUID): Int {
        return dao.getTotalPosts(newsGroup)
    }

    suspend fun createUser(
        name: String,
        bio: String,
        imageBitmap: Bitmap? = null,
        identity: UUID? = null
    ): User = withContext(
        Dispatchers.IO
    ) {
        requireConnected()
        val id = if (identity != null) {
            sdkComponent.binderWrapper.getIdentity(identity)!!
        } else {
            sdkComponent.binderWrapper.generateIdentity(name)
        }
        val user = User(
            identity = id.fingerprint,
            userName = name,
            bio = bio,
            owned = true,
            image = imageBitmap
        )
        val message = ScatterMessage.Builder.newInstance(context, user.bytes)
            .setApplication(context.getString(R.string.scatterbrainapplication))
            .build()
        sdkComponent.binderWrapper.sendMessage(message)
        dao.insertUsers(user)
        user
    }

    suspend fun insertUser(user: User) {
        dao.insertUsers(user)
    }

    private fun getUsersWithFiles(userlist: List<User>): LiveData<List<User>> = liveData {
        log.v("emit")
        emit(userlist)
    }

    fun observeUsers(owned: Boolean? = null): LiveData<List<User>> {
        return if (owned == null) {
            dao.observeAllUsers()
                .switchMap { userlist ->
                    log.v("fnmef")
                    getUsersWithFiles(userlist)
                }
        } else {
            dao.observeAllOwnedUsers(owned)
                .switchMap { userlist -> getUsersWithFiles(userlist) }
        }
    }

    fun observePosts(group: NewsGroup): LiveData<List<PostWithUsers>> {
        return dao.observePostsForGroup(group.uuid)
    }

    fun observeConnectionState(): LiveData<BinderWrapper.Companion.BinderState> {
        return sdkComponent.binderWrapper.observeBinderState()
    }

    suspend fun readUsers(owned: Boolean): List<User> {
        return dao.getAllOwnedUsers(owned)
    }

    suspend fun insertGroup(group: NewsGroup) {
        dao.insertGroup(group)
        sbSendGroup(group)
    }


    suspend fun insertGroup(group: List<NewsGroup>) {
        dao.insertGroups(group)
        val messages = group.map { g ->
            ScatterMessage.Builder.newInstance(context, g.bytes)
                .setApplication(context.getString(R.string.scatterbrainapplication))
                .build()
        }
        if (isConnected()) {
            sdkComponent.binderWrapper.sendMessage(messages)
        }
    }

    suspend fun deleteUser(user: UUID): Boolean {
        dao.getUsersByIdentity(user)
        return dao.deleteByIdentity(user) == 1
    }

    suspend fun createGroup(name: String, description: String, parent: NewsGroup): NewsGroup {
        val uuid = UUID.randomUUID()
        log.e("parent emptu: ${parent.empty}")
        val group = NewsGroup(
            uuid = uuid,
            parentCol = if (parent.empty) null else parent.uuid,
            groupName = name,
            parentHash = if (parent.empty) null else parent.hash,
            description = description
        )
        insertGroup(group)
        return group
    }

    suspend fun getChildren(group: UUID): List<NewsGroup> {
        val dbchild: NewsGroupChildren = dao.getGroupWithChildren(group)
        return dbchild?.children ?: ArrayList()
    }

    fun observeGroups(): LiveData<List<NewsGroup>> {
        return dao.observeAllGroups()
    }

    fun observeGroups(name: String): LiveData<List<NewsGroup>> {
        return dao.observeAllGroups(name)
    }


    fun observeChildren(group: UUID): LiveData<List<NewsGroup>> {
        return dao.observeGroupWithChildren(group)
            .map { children -> children.children }
    }

    private suspend fun processScatterMessages(messages: List<ScatterMessage>) {
        messages.forEach { message ->
            try {
                if (!message.isFile) {
                    val type = Message.parse(message.body!!)
                    log.v("processing message type ${type.typeVal}")
                    when (type.typeVal) {
                        TypeVal.POST -> {
                            val post = Message.parse<Post>(message.body!!, type)
                            dao.insertPost(post)
                        }

                        TypeVal.NEWSGROUP -> {
                            val newsgroup = Message.parse<NewsGroup>(message.body!!, type)
                            log.v("got newsgroup ${newsgroup.groupName} id=${newsgroup.uuid} parent=${newsgroup.parent}")
                            dao.insertGroup(newsgroup)
                        }

                        TypeVal.USER -> {
                            val user = Message.parse<User>(message.body!!, type)
                            insertUser(user)
                        }

                        else -> {
                            log.e("invalid message type received")
                        }
                    }
                }
            } catch (exc: Exception) {
                log.e("failed to process message from ${message.id}: $exc")
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
        val messages = withContext(Dispatchers.IO) {
            sdkComponent.binderWrapper.getScatterMessages(
                context.getString(R.string.scatterbrainapplication)
            )
        }
        withContext(Dispatchers.Default) { processScatterMessages(messages.toList()) }
        refreshInProgress.set(false)
        return true
    }

    suspend fun fullSync(since: Date): Boolean {
        requireConnected()
        if (refreshInProgress.getAndSet(true)) {
            return false
        }
        val messages = withContext(Dispatchers.IO) {
            sdkComponent.binderWrapper.getScatterMessages(context.getString(R.string.scatterbrainapplication))
        }.toList()

        processScatterMessages(messages)


//        val posts = dao.getAllPosts()
//        val groups = dao.getAllGroups()
//
//        for (post in posts) {
//            sbSendPost(post)
//        }
//
//        for (group in groups) {
//            sbSendGroup(group)
//        }

        refreshInProgress.set(false)
        return true
    }

    suspend fun fullSync(start: Date, end: Date): Boolean {
        requireConnected()
        if (refreshInProgress.getAndSet(true)) {
            return false
        }
        val messages = withContext(Dispatchers.IO) {
            sdkComponent.binderWrapper.getScatterMessages(
                context.getString(R.string.scatterbrainapplication),
                start,
                end
            )
        }
        withContext(Dispatchers.Default) { processScatterMessages(messages.toList()) }
        refreshInProgress.set(false)
        return true
    }

    companion object {
        const val TAG = "NewsRepository"
        const val PREF_LAST_SYNC_TIME = "last_sync_time"
    }

}