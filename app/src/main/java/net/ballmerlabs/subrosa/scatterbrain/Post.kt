package net.ballmerlabs.subrosa.scatterbrain

import android.util.Log
import androidx.room.*
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.isNotEmpty
import net.ballmerlabs.subrosa.util.HasKey
import net.ballmerlabs.subrosa.util.uuidConvertProto
import subrosaproto.Subrosa
import java.security.MessageDigest
import java.util.*

@Entity(
    tableName = "posts",
    indices = [
        Index("post_id", unique = true),
        Index("author", unique = false)
              ],
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["identity"], childColumns = ["author"])
    ]
)
class Post @Ignore constructor(
    packet: Subrosa.Post
): Message<Subrosa.Post>(packet), HasKey<String> {

    @Ignore
    override val typePacket: Subrosa.TypePrefix =  Subrosa.TypePrefix.newBuilder()
        .setPostType(toProto(TypeVal.POST))
        .build()

    @Embedded
    var parent: NewsGroup = NewsGroup.fromPacket(packet.parent)

    var author: UUID? = if(packet.hasAuthor()) { uuidConvertProto(packet.author) } else { null }

    var header: String = packet.header

    var body: String = packet.body

    var sig: ByteArray? = if (packet.sig.isNotEmpty())
        packet.sig.toByteArray()
    else
        null

    @ColumnInfo(name = "post_id")
    var postId: UUID? = uuidConvertProto(packet.uuid)

    @ColumnInfo(defaultValue = 0.toString())
    var receivedDate: Long = Date().time

    @PrimaryKey() var id: String = hasKey()

    constructor(
        parent: NewsGroup,
        author: UUID? = null,
        header: String,
        body: String,
        sig: ByteArray? = null,
        postId: UUID? = null,
        receivedDate: Long = Date().time,
        id: String = ""
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