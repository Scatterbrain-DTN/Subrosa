package net.ballmerlabs.subrosa.scatterbrain

import com.google.protobuf.ByteString
import net.ballmerlabs.subrosa.SubrosaProto
import java.util.*

data class Parent(
    val parentUUID: UUID,
    val parentHash: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Parent

        if (parentUUID != other.parentUUID) return false
        if (!parentHash.contentEquals(other.parentHash)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = parentUUID.hashCode()
        result = 31 * result + parentHash.contentHashCode()
        return result
    }
}


class NewsGroup(
    packet: SubrosaProto.NewsGroup
): Message<SubrosaProto.NewsGroup>(packet) {
    val uuid
    get() = uuidConvert(packet.uuid)

    val parent
    get() = if(hasParent)
        Parent(uuidConvert(packet.parent.parentuuid), packet.parent.parenthash.toByteArray())
    else
        null

    val hasParent
    get() = packet.parentOptionCase == SubrosaProto.NewsGroup.ParentOptionCase.PARENT

    val isTopLevel
    get() = packet.parentOptionCase == SubrosaProto.NewsGroup.ParentOptionCase.TOPLEVEL


    val name
    get() = packet.name

    constructor(
        uuid: UUID,
        parent: Parent?,
        name: String
    ): this(
        if (parent == null)
            SubrosaProto.NewsGroup.newBuilder()
                .setToplevel(true)
                .setName(name)
                .setUuid(uuidConvert(uuid))
                .build()
        else
            SubrosaProto.NewsGroup.newBuilder()
                .setParent(
                    SubrosaProto.Parent.newBuilder()
                        .setParenthash(ByteString.copyFrom(parent.parentHash))
                        .setParentuuid(uuidConvert(parent.parentUUID))
                        .build()
                )
                .setName(name)
                .setUuid(uuidConvert(uuid))
                .build()
    )

    companion object {
        class Parser: Message.Companion.Parser<SubrosaProto.NewsGroup, NewsGroup>(SubrosaProto.NewsGroup.parser())
    }

}