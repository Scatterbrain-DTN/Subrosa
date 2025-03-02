package net.ballmerlabs.subrosa.scatterbrain

import android.util.Log
import androidx.room.*
import com.google.protobuf.ByteString
import net.ballmerlabs.subrosa.util.HasKey
import net.ballmerlabs.subrosa.util.uuidConvertProto
import subrosaproto.Subrosa
import java.security.MessageDigest
import java.util.*

@Entity(
    tableName = "posts",
    indices = [ Index("post_id", unique = true) ]
)
class Post(
    packet: Subrosa.Post
): Message<Subrosa.Post>(packet), HasKey<String> {

    @Ignore
    override val typePacket: Subrosa.TypePrefix =  Subrosa.TypePrefix.newBuilder()
        .setPostType(toProto(TypeVal.POST))
        .build()

    @Embedded
    var parent: NewsGroup = NewsGroup(packet.parent)

    @Embedded
    var user: User? = null

    var author: UUID? = if(packet.hasAuthor()) { uuidConvertProto(packet.author) } else { null }

    var header = packet.header

    var body = packet.body

    var sig = packet.sig.toByteArray()

    @ColumnInfo(name = "post_id")
    var postId: UUID? = uuidConvertProto(packet.uuid)

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
            Subrosa.Post.newBuilder()
                .setParent(parent.packet)
                .setAuthor(uuidConvertProto(author))
                .setHeader(header)
                .setBody(body)
                .setSig(ByteString.copyFrom(sig))
                .setUuid(uuidConvertProto(UUID.randomUUID()))
                .build()
        } else {
            Subrosa.Post.newBuilder()
                .setParent(parent.packet)
                .setHeader(header)
                .setBody(body)
                .setUuid(uuidConvertProto(UUID.randomUUID()))
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
        class Parser: Message.Companion.Parser<Subrosa.Post, Post>(Subrosa.Post.parser()) {
            override val type: Subrosa.PostType = Subrosa.PostType.POST
        }
        val parser = Parser()
    }
}