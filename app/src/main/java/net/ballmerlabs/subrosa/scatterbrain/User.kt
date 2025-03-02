package net.ballmerlabs.subrosa.scatterbrain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.protobuf.ByteString
import kotlinx.coroutines.*
import net.ballmerlabs.subrosa.util.HasKey
import net.ballmerlabs.subrosa.util.uuidConvertProto
import subrosaproto.Subrosa
import java.io.ByteArrayOutputStream
import java.util.*

@Entity
class User(
    packet: Subrosa.User
) : Message<Subrosa.User>(packet), HasKey<UUID> {

    @PrimaryKey
    var identity: UUID = uuidConvertProto(packet.identity)

    var userName: String = packet.name

    var bio: String = packet.bio

    var owned: Boolean = false

    var imageBytes: ByteArray? =
        if (packet.imageCase.equals(Subrosa.User.ImageCase.IMAGE_NOT_SET))
            null
        else
            packet.imagebytes.toByteArray()

    @Ignore
    override val typePacket: Subrosa.TypePrefix = Subrosa.TypePrefix.newBuilder()
        .setPostType(toProto(TypeVal.USER))
        .build()

    override fun hasKey(): UUID {
        return identity
    }

    override fun toString(): String {
        return userName
    }

    fun decodeImage(): Bitmap? {
        val imageBytes = this.imageBytes
        return if (imageBytes == null) {
            null
        } else {
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }
    }

    constructor(
        identity: UUID,
        userName: String,
        bio: String,
        owned: Boolean = false,
    ) : this(
        compress(null)
            .setIdentity(uuidConvertProto(identity))
            .setBio(bio)
            .setName(userName)
            .build()
    ) {
        this.owned = owned
    }

    constructor(
        identity: UUID,
        userName: String,
        bio: String,
        owned: Boolean = false,
        image: Bitmap? = null
    ) : this(
        compress(image)
            .setIdentity(uuidConvertProto(identity))
            .setBio(bio)
            .setName(userName)
            .build()
    ) {
        this.owned = owned
    }


    companion object {
        fun compress(bitmap: Bitmap?): Subrosa.User.Builder {
            val builder = Subrosa.User.newBuilder()
            return if (bitmap != null) {
                val os = ByteArrayOutputStream()
                resizeBitmapCentered(bitmap, 512).compress(Bitmap.CompressFormat.PNG, 90, os)
                builder.setImagebytes(ByteString.copyFrom(os.toByteArray()))
            } else {
                builder
            }
        }


        fun resizeBitmapCentered(bitmap: Bitmap, width: Int): Bitmap {
            val scaledsize = bitmap.width.coerceAtMost(bitmap.height)
            val wstart = bitmap.width - scaledsize
            val hstart = bitmap.height - scaledsize
            val new = Bitmap.createBitmap(bitmap, wstart, hstart, scaledsize, scaledsize)
            return new.scale(width, width)
        }

        class Parser :
            Message.Companion.Parser<Subrosa.User, User>(Subrosa.User.parser()) {
            override val type: Subrosa.PostType = Subrosa.PostType.USER
        }

        val parser = Parser()
    }
}