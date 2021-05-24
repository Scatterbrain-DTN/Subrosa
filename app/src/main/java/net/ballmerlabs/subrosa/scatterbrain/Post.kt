package net.ballmerlabs.subrosa.scatterbrain

import androidx.room.*
import com.google.protobuf.ByteString
import net.ballmerlabs.subrosa.SubrosaProto

@Entity(
    tableName = "posts",
    foreignKeys = [
        ForeignKey(
            entity = NewsGroup::class,
            parentColumns = ["uuid"],
            childColumns = ["parent"],
            onDelete = ForeignKey.CASCADE
        )
    ])
class Post(
    packet: SubrosaProto.Post
): Message<SubrosaProto.Post>(packet) {

    @Ignore
    override val type: TypeVal = TypeVal.POST

    @Embedded
    var parent: NewsGroup = NewsGroup(packet.parent)

    @Ignore
    val parentObj = packet.parent

    var author = packet.author

    var header = packet.header

    var body = packet.body

    var sig = packet.sig.toByteArray()

    @PrimaryKey(autoGenerate = true) var id = 0

    constructor(
        parent: NewsGroup,
        author: String,
        header: String,
        body: String,
        sig: ByteArray
    ): this(
        SubrosaProto.Post.newBuilder()
            .setParent(parent.packet)
            .setAuthor(author)
            .setHeader(header)
            .setBody(body)
            .setSig(ByteString.copyFrom(sig))
            .build()
    )


    companion object {
        class Parser: Message.Companion.Parser<SubrosaProto.Post, Post>(SubrosaProto.Post.parser()) {
            override val type: SubrosaProto.Type.PostType = SubrosaProto.Type.PostType.POST
        }
        val parser = Parser()
    }
}