package net.ballmerlabs.subrosa

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.SdkComponent
import net.ballmerlabs.subrosa.database.NewsGroupDao
import javax.inject.Inject

class NewsRepository @Inject constructor(
    @ApplicationContext val context: Context,
    val dao: NewsGroupDao,
    val sdkComponent: SdkComponent
    ) {

}