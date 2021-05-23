package net.ballmerlabs.subrosa

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import net.ballmerlabs.subrosa.database.NewsGroupDao
import net.ballmerlabs.subrosa.database.Post
import javax.inject.Inject

class NewsRepository @Inject constructor(
    @ApplicationContext val context: Context,
    val dao: NewsGroupDao,
    val sdkComponent: ScatterbrainApi
    ) {

    suspend fun sendPost(post: Post) {
        
    }
}