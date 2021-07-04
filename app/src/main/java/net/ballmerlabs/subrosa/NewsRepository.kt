package net.ballmerlabs.subrosa

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.yield
import net.ballmerlabs.scatterbrainsdk.ScatterMessage
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import net.ballmerlabs.subrosa.database.NewsGroupChildren
import net.ballmerlabs.subrosa.database.NewsGroupDao
import net.ballmerlabs.subrosa.scatterbrain.Message
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.scatterbrain.Post
import net.ballmerlabs.subrosa.scatterbrain.TypeVal
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

class NewsRepository @Inject constructor(
    @ApplicationContext val context: Context,
    val dao: NewsGroupDao,
    val sdkComponent: ScatterbrainApi
    ) {

    init {
        sdkComponent.broadcastReceiver.register()
    }
    suspend fun sendPost(post: Post) {
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
        sdkComponent.binderWrapper.sendMessage(groupMsgs)
        sdkComponent.binderWrapper.sendMessage(message, post.author)
    }

    suspend fun insertGroup(group: NewsGroup) {
        val message = ScatterMessage.newBuilder()
            .setApplication(context.getString(R.string.scatterbrainapplication))
            .setBody(group.bytes)
            .build()
        dao.insertGroup(group)
        if (sdkComponent.binderWrapper.isConnected()) {
            sdkComponent.binderWrapper.sendMessage(message)
        }
    }


    suspend fun insertGroup(group: List<NewsGroup>) {
        dao.insertGroups(group)
        val messages = group.map { g ->
            ScatterMessage.newBuilder()
                .setApplication(context.getString(R.string.scatterbrainapplication))
                .setBody(g.bytes)
                .build()
        }
        if (sdkComponent.binderWrapper.isConnected()) {
            sdkComponent.binderWrapper.sendMessage(messages)
        }
    }

    suspend fun createGroup(name: String, parent: NewsGroup): NewsGroup {
        val uuid = UUID.randomUUID()
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
        val dbchild: NewsGroupChildren? = dao.getGroupWithChildren(group)
        return dbchild?.children?: ArrayList()
    }

    suspend fun observePosts(): Flow<Post> = flow {
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