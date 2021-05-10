package net.ballmerlabs.subrosa

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext

class NewsRepository(@ApplicationContext val context: Context) {
}