package net.ballmerlabs.subrosa

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import net.ballmerlabs.scatterbrainsdk.ScatterMessage
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import net.ballmerlabs.subrosa.database.NewsGroupDao
import net.ballmerlabs.subrosa.scatterbrain.Post
import javax.inject.Inject

class NewsRepository @Inject constructor(
    @ApplicationContext val context: Context,
    val dao: NewsGroupDao,
    val sdkComponent: ScatterbrainApi
    ) {

    suspend fun sendPost(post: Post) {
        val parent = dao.getGroup(post.parent.uuid)
        val message = ScatterMessage.newBuilder()
            .setApplication(context.getString(R.string.scatterbrainapplication))
            .setBody(byteArrayOf(0))
            .build()
    }
}