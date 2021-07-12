package net.ballmerlabs.subrosa.scatterbrain

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import net.ballmerlabs.scatterbrainsdk.ScatterbrainBroadcastReceiver
import javax.inject.Named
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class ScatterbrainModule {
    @Provides
    @Singleton
    fun providesDaggerComponent(@ApplicationContext context: Context): ScatterbrainApi {
        return ScatterbrainApi(context)
    }

    @Provides
    @Singleton
    fun providesBinderWrapper(api: ScatterbrainApi): BinderWrapper {
        return api.binderWrapper
    }

    @Provides
    @Singleton
    fun providesBroadcastReceiver(api: ScatterbrainApi): ScatterbrainBroadcastReceiver {
        return api.broadcastReceiver
    }


    @Named(API_COROUTINE_SCOPE)
    @Provides
    @Singleton
    fun providesCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    companion object {
        const val API_COROUTINE_SCOPE = "apiscope"
    }
}