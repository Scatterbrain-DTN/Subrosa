package net.ballmerlabs.subrosa.scatterbrain

import com.google.protobuf.ByteString
import net.ballmerlabs.subrosa.SubrosaProto
import java.util.*

class Post(
    packet: SubrosaProto.Post
): Message<SubrosaProto.Post>(packet) {
    val parent
    get() = packet.parent

    val author
    get() = uuidConvert(packet.author)

    val header
    get() = packet.header

    val body
    get() = packet.body

    val sig
    get() = packet.sig.toByteArray()


    constructor(
        parent: NewsGroup,
        author: UUID,
        header: String,
        body: String,
        sig: ByteArray
    ): this(
        SubrosaProto.Post.newBuilder()
            .setParent(parent.packet)
            .setAuthor(uuidConvert(author))
            .setHeader(header)
            .setBody(body)
            .setSig(ByteString.copyFrom(sig))
            .build()
    )


    companion object {
        class Parser: Message.Companion.Parser<SubrosaProto.Post, Post>(SubrosaProto.Post.parser())
    }
}