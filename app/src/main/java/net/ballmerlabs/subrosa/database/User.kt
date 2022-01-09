package net.ballmerlabs.subrosa.database

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.coroutines.*
import java.io.File
import java.util.*

@Entity
class User(
    @PrimaryKey val identity: UUID,
    val name: String,
    val bio: String,
    val imagePath: String = "$identity.png",
    val owned: Boolean = false,
){
    @Ignore var image: Bitmap? = null
    suspend fun getImageFromPath(context: Context): Boolean = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, imagePath)
        if (file.exists()) {
            image = BitmapFactory.decodeStream(file.inputStream())
            true
        } else {
            false
        }
    }

    suspend fun writeImage(bitmap: Bitmap, context: Context) = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, imagePath)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, file.outputStream())
    }

    override fun toString(): String {
        return name
    }
}