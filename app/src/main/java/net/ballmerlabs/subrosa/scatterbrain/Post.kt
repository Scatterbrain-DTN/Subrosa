package net.ballmerlabs.subrosa.scatterbrain

import com.google.protobuf.ByteString
import net.ballmerlabs.subrosa.SubrosaProto
import java.util.*

class Post(
    val post: SubrosaProto.Post
) {
    val parent
    get() = post.parent

    val author
    get() = uuidConvert(post.author)

    val header
    get() = post.header

    val body
    get() = post.body

    val sig
    get() = post.sig.toByteArray()


    constructor(
        parent: NewsGroup,
        author: UUID,
        header: String,
        body: String,
        sig: ByteArray
    ): this(
        SubrosaProto.Post.newBuilder()
            .setParent(parent.newsGroup)
            .setAuthor(uuidConvert(author))
            .setHeader(header)
            .setBody(body)
            .setSig(ByteString.copyFrom(sig))
            .build()
    )
}