package net.ballmerlabs.subrosa

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.yield
import net.ballmerlabs.scatterbrainsdk.ScatterMessage
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import net.ballmerlabs.subrosa.database.NewsGroupDao
import net.ballmerlabs.subrosa.scatterbrain.Message
import net.ballmerlabs.subrosa.scatterbrain.Post
import javax.inject.Inject

class NewsRepository @Inject constructor(
    @ApplicationContext val context: Context,
    val dao: NewsGroupDao,
    val sdkComponent: ScatterbrainApi
    ) {

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
                    val post = Message.parse(Post.parser, message.body)
                    emit(post)
                }
            }
    }

}