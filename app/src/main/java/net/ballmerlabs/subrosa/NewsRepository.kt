package net.ballmerlabs.subrosa

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import com.lelloman.identicon.drawable.GithubIdenticonDrawable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import net.ballmerlabs.scatterbrainsdk.ScatterMessage
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import net.ballmerlabs.subrosa.database.NewsGroupChildren
import net.ballmerlabs.subrosa.database.NewsGroupDao
import net.ballmerlabs.subrosa.database.User
import net.ballmerlabs.subrosa.scatterbrain.*
import java.lang.IllegalStateException
import java.util.*
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

    private val connectedCallback: MutableSet<(connected: Boolean) -> Unit> = HashSet()
    private var isConnected = false

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

    suspend fun sendPost(post: Post) {
        updateConnected()
        val message = ScatterMessage.newBuilder()
            .setApplication(context.getString(R.string.scatterbrainapplication))
            .setBody(post.bytes)
            .setFrom(post.author)
            .build()
        var par = post.parent
        val groupMsgs = ArrayList<ScatterMessage>()
        while (par.hasParent) {
            yield()
            par = dao.getGroup(par.parentCol!!)
            val groupMsg = ScatterMessage.newBuilder()
                .setApplication(context.getString(R.string.scatterbrainapplication))
                .setBody(par.bytes)
                .build()
            groupMsgs.add(groupMsg)
        }
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

    suspend fun readUsers(owned: Boolean): List<User> {
        updateConnected()
        return dao.getAllOwnedUsers(owned)
    }

    suspend fun readUsers(uuid: UUID): User {
        updateConnected()
        return dao.getUsersByIdentity(uuid)
    }

    suspend fun insertGroup(group: NewsGroup) {
        updateConnected()
        val message = ScatterMessage.newBuilder()
            .setApplication(context.getString(R.string.scatterbrainapplication))
            .setBody(group.bytes)
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
            ScatterMessage.newBuilder()
                .setApplication(context.getString(R.string.scatterbrainapplication))
                .setBody(g.bytes)
                .build()
        }
        if (isConnected()) {
            sdkComponent.binderWrapper.sendMessage(messages)
        }
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

    suspend fun observePosts(): Flow<Post> = flow {
        requireConnected()
        sdkComponent.binderWrapper.observeMessages(context.getString(R.string.scatterbrainapplication))
            .map { messages ->
                for (message in messages) {
                    if (!message.toDisk) {
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

    suspend fun fullSync() {
        requireConnected()
        sdkComponent.binderWrapper.getScatterMessages(context.getString(R.string.scatterbrainapplication))
            .forEach { message ->
                if (!message.toDisk) {
                    val type = Message.parse(message.body!!)
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
            }
    }

    companion object {
        const val TAG = "NewsRepository"
    }

}