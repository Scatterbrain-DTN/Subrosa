package net.ballmerlabs.subrosa.scatterbrain

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import net.ballmerlabs.scatterbrainsdk.ScatterbrainBroadcastReceiver

@InstallIn(SingletonComponent::class)
@Module
class ScatterbrainModule {
    @Provides
    fun providesDaggerComponent(@ApplicationContext context: Context): ScatterbrainApi {
        return ScatterbrainApi(context)
    }

    @Provides
    fun providesBinderWrapper(api: ScatterbrainApi): BinderWrapper {
        return api.binderWrapper
    }

    @Provides
    fun providesBroadcastReceiver(api: ScatterbrainApi): ScatterbrainBroadcastReceiver {
        return api.broadcastReceiver
    }
}