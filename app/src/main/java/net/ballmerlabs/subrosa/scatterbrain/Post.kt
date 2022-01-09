package net.ballmerlabs.subrosa.scatterbrain

import androidx.room.*
import com.google.protobuf.ByteString
import net.ballmerlabs.subrosa.SubrosaProto
import net.ballmerlabs.subrosa.database.User
import net.ballmerlabs.subrosa.util.HasKey
import net.ballmerlabs.subrosa.util.uuidConvert
import net.ballmerlabs.subrosa.util.uuidConvertProto
import java.security.MessageDigest
import java.util.*

@Entity(
    tableName = "posts"
)
class Post(
    packet: SubrosaProto.Post
): Message<SubrosaProto.Post>(packet), HasKey<Int> {

    @Ignore
    override val typePacket: SubrosaProto.Type =  SubrosaProto.Type.newBuilder()
        .setType(toProto(TypeVal.POST))
        .build()

    @Embedded
    var parent: NewsGroup = NewsGroup(packet.parent)

    @Embedded
    var user: User? = null

    var author = uuidConvertProto(packet.author)

    var header = packet.header

    var body = packet.body

    var sig = packet.sig.toByteArray()

    @PrimaryKey(autoGenerate = true) var id = 0

    constructor(
        parent: NewsGroup,
        author: UUID,
        header: String,
        body: String,
        sig: ByteArray,
    ): this(
        SubrosaProto.Post.newBuilder()
            .setParent(parent.packet)
            .setAuthor(uuidConvertProto(author))
            .setHeader(header)
            .setBody(body)
            .setSig(ByteString.copyFrom(sig))
            .build()
    )

    override fun hasKey(): Int {
        return id
    }


    companion object {
        class Parser: Message.Companion.Parser<SubrosaProto.Post, Post>(SubrosaProto.Post.parser()) {
            override val type: SubrosaProto.Type.PostType = SubrosaProto.Type.PostType.POST
        }
        val parser = Parser()
    }
}