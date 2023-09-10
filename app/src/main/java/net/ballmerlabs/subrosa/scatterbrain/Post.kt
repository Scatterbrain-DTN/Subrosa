package net.ballmerlabs.subrosa.scatterbrain

import android.util.Log
import androidx.room.*
import com.google.protobuf.ByteString
import net.ballmerlabs.subrosa.SubrosaProto
import net.ballmerlabs.subrosa.util.HasKey
import net.ballmerlabs.subrosa.util.srLog
import net.ballmerlabs.subrosa.util.uuidConvertProto
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

@Entity(
    tableName = "posts"
)
class Post(
    packet: SubrosaProto.Post
): Message<SubrosaProto.Post>(packet), HasKey<String> {

    @Ignore
    override val typePacket: SubrosaProto.Type =  SubrosaProto.Type.newBuilder()
        .setType(toProto(TypeVal.POST))
        .build()

    @Embedded
    var parent: NewsGroup = NewsGroup(packet.parent)

    @Embedded
    var user: User? = null

    var author: UUID? = if(packet.hasAuthor()) { uuidConvertProto(packet.author) } else { null }

    var header = packet.header

    var body = packet.body

    var sig = packet.sig.toByteArray()

    @ColumnInfo(defaultValue = 0.toString())
    var receivedDate: Long = Date().time

    @PrimaryKey() var id: String = hasKey()

    constructor(
        parent: NewsGroup,
        author: UUID?,
        header: String,
        body: String,
        sig: ByteArray?,
    ): this(
        if (author != null) {
            SubrosaProto.Post.newBuilder()
                .setParent(parent.packet)
                .setAuthor(uuidConvertProto(author))
                .setHeader(header)
                .setBody(body)
                .setSig(ByteString.copyFrom(sig))
                .build()
        } else {
            SubrosaProto.Post.newBuilder()
                .setParent(parent.packet)
                .setHeader(header)
                .setBody(body)
                .build()
        }
    )

    override fun hasKey(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(packet.toByteArray())
        val base64 = Base64.getEncoder().encodeToString(digest.digest())
        Log.e("debug", "digest $base64")
        return base64
    }


    companion object {
        class Parser: Message.Companion.Parser<SubrosaProto.Post, Post>(SubrosaProto.Post.parser()) {
            override val type: SubrosaProto.Type.PostType = SubrosaProto.Type.PostType.POST
        }
        val parser = Parser()
    }
}