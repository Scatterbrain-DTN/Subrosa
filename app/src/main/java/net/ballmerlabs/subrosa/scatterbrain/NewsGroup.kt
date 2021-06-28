package net.ballmerlabs.subrosa.scatterbrain

import android.os.Parcel
import android.os.ParcelUuid
import android.os.Parcelable
import androidx.room.*
import com.google.protobuf.ByteString
import net.ballmerlabs.subrosa.SubrosaProto
import net.ballmerlabs.subrosa.util.uuidConvert
import java.security.MessageDigest
import java.util.*

data class Parent(
    val parentUUID: UUID,
    val parentHash: ByteArray,
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable<ParcelUuid>(ParcelUuid::class.java.classLoader)!!.uuid,
        parcel.createByteArray()!!
    ) {
    }

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

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(ParcelUuid(parentUUID), flags)
        parcel.writeByteArray(parentHash)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Parent> {
        override fun createFromParcel(parcel: Parcel): Parent {
            return Parent(parcel)
        }

        override fun newArray(size: Int): Array<Parent?> {
            return arrayOfNulls(size)
        }
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
): Message<SubrosaProto.NewsGroup>(packet), Parcelable {

    @Ignore
    override val typePacket: SubrosaProto.Type = SubrosaProto.Type.newBuilder()
        .setType(toProto(TypeVal.NEWSGROUP))
        .build()

    @PrimaryKey
    var uuid: UUID = uuidConvertProto(packet.uuid)

    @Ignore
    val hasParent = packet.parentOptionCase == SubrosaProto.NewsGroup.ParentOptionCase.PARENT

    var parentHash: ByteArray? = if (hasParent) packet.parent.parenthash.toByteArray() else null

    @Ignore
    val hash: ByteArray

    @ColumnInfo(name = "parent")
    var parentCol: UUID = if (hasParent) uuidConvertProto(packet.parent.parentuuid) else uuid

    @Ignore
    val parent = if(hasParent)
        Parent(uuidConvertProto(packet.parent.parentuuid), packet.parent.parenthash.toByteArray())
    else
        null

    @Ignore
    val isTopLevel = packet.parentOptionCase == SubrosaProto.NewsGroup.ParentOptionCase.TOPLEVEL


    var name = packet.name


    init {
        hash = MessageDigest.getInstance("SHA-256").digest(
            uuidConvert(uuid) + (parentHash?: ByteArray(0))
        )
    }

    constructor(parcel: Parcel): this(
        name = parcel.readString()!!,
        uuid = parcel.readParcelable<ParcelUuid>(ParcelUuid::class.java.classLoader)!!.uuid,
        parentCol = parcel.readParcelable<ParcelUuid>(ParcelUuid::class.java.classLoader)!!.uuid,
        parentHash = parcel.createByteArray()!!
    )

    constructor(
        uuid: UUID,
        parentCol: UUID,
        name: String,
        parentHash: ByteArray
    ): this(
        if (parentCol == uuid)
            SubrosaProto.NewsGroup.newBuilder()
                .setToplevel(true)
                .setName(name)
                .setUuid(uuidConvertProto(uuid))
                .build()
        else
            SubrosaProto.NewsGroup.newBuilder()
                .setParent(
                    SubrosaProto.Parent.newBuilder()
                        .setParenthash(ByteString.copyFrom(parentHash))
                        .setParentuuid(uuidConvertProto(parentCol))
                        .build()
                )
                .setName(name)
                .setUuid(uuidConvertProto(uuid))
                .build()
    )


    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(packet.name)
        dest.writeParcelable(ParcelUuid(uuidConvertProto(packet.uuid)), flags)
        if (packet.parentOptionCase == SubrosaProto.NewsGroup.ParentOptionCase.PARENT) {
            dest.writeParcelable(ParcelUuid(uuidConvertProto(packet.parent.parentuuid)), flags)
            dest.writeByteArray(packet.parent.parenthash.toByteArray())
        } else {
            dest.writeParcelable(ParcelUuid(uuidConvertProto(packet.uuid)), flags)
            dest.writeByteArray(ByteArray(0))
        }
    }


    companion object CREATOR : Parcelable.Creator<NewsGroup> {
        class Parser: Companion.Parser<SubrosaProto.NewsGroup, NewsGroup>(SubrosaProto.NewsGroup.parser()) {
            override val type: SubrosaProto.Type.PostType = SubrosaProto.Type.PostType.NEWSGROUP
        }
        val parser = Parser()

        override fun createFromParcel(parcel: Parcel): NewsGroup {
            return NewsGroup(parcel)
        }

        override fun newArray(size: Int): Array<NewsGroup?> {
            return arrayOfNulls(size)
        }

    }
}