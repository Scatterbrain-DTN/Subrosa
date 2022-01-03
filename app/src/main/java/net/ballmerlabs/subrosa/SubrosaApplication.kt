package net.ballmerlabs.subrosa

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SubrosaApplication : Application() {
    companion object {
        const val SHARED_PREFS_DEFAULT = "defaultprefs"
    }
}