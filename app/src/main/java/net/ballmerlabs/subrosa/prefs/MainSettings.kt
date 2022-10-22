package net.ballmerlabs.subrosa.prefs

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import net.ballmerlabs.subrosa.R
import javax.inject.Inject

@AndroidEntryPoint
class MainSettings @Inject constructor() : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

    }
}