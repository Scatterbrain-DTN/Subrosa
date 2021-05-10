package net.ballmerlabs.subrosa.scatterbrain

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.DaggerSdkComponent
import net.ballmerlabs.scatterbrainsdk.ScatterbrainBroadcastReceiver
import net.ballmerlabs.scatterbrainsdk.SdkComponent

@InstallIn(SingletonComponent::class)
@Module
class ScatterbrainModule {
    @Provides
    fun providesDaggerComponent(@ApplicationContext context: Context): SdkComponent {
        return DaggerSdkComponent.builder()
            .applicationContext(context)!!
            .build()!!
    }

    @Provides
    fun providesBinderWrapper(sdkComponent: SdkComponent): BinderWrapper {
        return sdkComponent.sdk()
    }

    @Provides
    fun providesBroadcastReceiver(sdkComponent: SdkComponent): ScatterbrainBroadcastReceiver {
        return sdkComponent.broadcastReceiver()
    }
}