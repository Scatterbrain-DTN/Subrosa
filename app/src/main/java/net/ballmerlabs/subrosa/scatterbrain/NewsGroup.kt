package net.ballmerlabs.subrosa.scatterbrain

import androidx.room.*
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

@Entity(
    tableName = "newsgroup",
    indices = [
        Index(
            value = ["parent"]
        )
    ],
    foreignKeys = [
        ForeignKey(
            entity = NewsGroup::class,
            parentColumns = ["uuid"],
            childColumns = ["parent"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class NewsGroup(
    packet: SubrosaProto.NewsGroup
): Message<SubrosaProto.NewsGroup>(packet) {

    @Ignore
    override val typePacket: SubrosaProto.Type = SubrosaProto.Type.newBuilder()
        .setType(toProto(TypeVal.NEWSGROUP))
        .build()

    @PrimaryKey
    var uuid: UUID = uuidConvert(packet.uuid)

    @Ignore
    val hasParent = packet.parentOptionCase == SubrosaProto.NewsGroup.ParentOptionCase.PARENT

    var parentHash: ByteArray? = if (hasParent) packet.parent.parenthash.toByteArray() else null

    @ColumnInfo(name = "parent")
    var parentCol: UUID = if (hasParent) uuidConvert(packet.parent.parentuuid) else uuid

    @Ignore
    val parent = if(hasParent)
        Parent(uuidConvert(packet.parent.parentuuid), packet.parent.parenthash.toByteArray())
    else
        null

    @Ignore
    val isTopLevel = packet.parentOptionCase == SubrosaProto.NewsGroup.ParentOptionCase.TOPLEVEL


    var name = packet.name

    constructor(
        uuid: UUID,
        parentCol: UUID,
        name: String,
        parentHash: ByteArray
    ): this(
        if (parentCol.equals(uuid))
            SubrosaProto.NewsGroup.newBuilder()
                .setToplevel(true)
                .setName(name)
                .setUuid(uuidConvert(uuid))
                .build()
        else
            SubrosaProto.NewsGroup.newBuilder()
                .setParent(
                    SubrosaProto.Parent.newBuilder()
                        .setParenthash(ByteString.copyFrom(parentHash))
                        .setParentuuid(uuidConvert(parentCol))
                        .build()
                )
                .setName(name)
                .setUuid(uuidConvert(uuid))
                .build()
    )

    companion object {
        class Parser: Message.Companion.Parser<SubrosaProto.NewsGroup, NewsGroup>(SubrosaProto.NewsGroup.parser()) {
            override val type: SubrosaProto.Type.PostType = SubrosaProto.Type.PostType.NEWSGROUP
        }
        val parser = Parser()
    }

}