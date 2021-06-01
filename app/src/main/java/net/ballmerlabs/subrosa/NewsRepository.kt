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
import net.ballmerlabs.subrosa.database.NewsGroupDao
import net.ballmerlabs.subrosa.scatterbrain.Message
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.scatterbrain.Post
import net.ballmerlabs.subrosa.scatterbrain.TypeVal
import javax.inject.Inject

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
            par = dao.getGroup(par.parentCol)
            val groupMsg = ScatterMessage.newBuilder()
                .setApplication(context.getString(R.string.scatterbrainapplication))
                .setBody(par.bytes)
                .build()
            groupMsgs.add(groupMsg)
        }
        sdkComponent.binderWrapper.sendMessage(groupMsgs)
        sdkComponent.binderWrapper.sendMessage(message, post.author)
    }

    suspend fun observePosts(): Flow<Post> = flow {
        sdkComponent.binderWrapper.observeMessages(context.getString(R.string.scatterbrainapplication))
            .map { messages ->
                for (message in messages) {
                    val type = Message.parse(message.body)
                    when (type.typeVal) {
                        TypeVal.POST -> {
                            val post = Message.parse<Post>(message.body, type)
                            dao.insertPost(post)
                            emit(post)
                        }
                        else -> { yield() }
                    }
                }
            }
    }

    suspend fun fullSync() {
        sdkComponent.binderWrapper.getScatterMessages(context.getString(R.string.scatterbrainapplication))
            .forEach { message ->
                val type = Message.parse(message.body)
                when(type.typeVal) {
                    TypeVal.POST -> {
                        val post = Message.parse<Post>(message.body, type)
                        dao.insertPost(post)
                    }
                    TypeVal.NEWSGROUP -> {
                        val newsgroup = Message.parse<NewsGroup>(message.body, type)
                        dao.insertGroup(newsgroup)
                    }
                    else -> {
                        Log.e(TAG, "invalid message type received")
                    }
                }
            }
    }

    companion object {
        const val TAG = "NewsRepository"
    }

}