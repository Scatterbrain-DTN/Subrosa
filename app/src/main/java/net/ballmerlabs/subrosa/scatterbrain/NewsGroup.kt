package net.ballmerlabs.subrosa.scatterbrain

import android.os.Parcel
import android.os.ParcelUuid
import android.os.Parcelable
import androidx.room.*
import com.google.protobuf.ByteString
import net.ballmerlabs.subrosa.util.HasKey
import net.ballmerlabs.subrosa.util.uuidConvert
import net.ballmerlabs.subrosa.util.uuidConvertProto
import subrosaproto.Subrosa
import java.security.MessageDigest
import java.util.*

data class Parent(
    val parentUUID: UUID,
    val parentHash: ByteArray,
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable<ParcelUuid>(ParcelUuid::class.java.classLoader)!!.uuid,
        parcel.createByteArray()!!
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Parent

        if (parentUUID != other.parentUUID) return false
        if (!parentHash.contentEquals(other.parentHash)) return false

        return true
    }

    override fun hashCode(): Int {
        val result = 31 *  parentHash.contentHashCode() + parentUUID.leastSignificantBits + parentUUID.mostSignificantBits
        return result.mod(Int.MAX_VALUE)
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
class NewsGroup @Ignore constructor(
    packet: Subrosa.NewsGroup
): Message<Subrosa.NewsGroup>(packet), Parcelable, HasKey<UUID> {
    @Ignore
    override val typePacket: Subrosa.TypePrefix = Subrosa.TypePrefix.newBuilder()
        .setPostType(toProto(TypeVal.NEWSGROUP))
        .build()

    @Ignore
    var empty = false

    @PrimaryKey
    var uuid: UUID = uuidConvertProto(packet.uuid)
    get() = checkEmpty(field)

    @ColumnInfo(defaultValue = "")
    var description: String = packet.description

    val hasParent: Boolean
    get() = packet.parentOptionCase == Subrosa.NewsGroup.ParentOptionCase.PARENT

    var parentHash: ByteArray? = if (hasParent) packet.parent.parenthash.toByteArray() else null

    val hash: ByteArray
    get() = checkEmpty(MessageDigest.getInstance("SHA-256").digest(
        uuidConvert(uuid) + (parentHash?: ByteArray(0)))
    )

    @ColumnInfo(name = "parent")
    var parentCol: UUID? = if (hasParent) uuidConvertProto(packet.parent.parentuuid) else null

    @Ignore
    val parent = if(hasParent)
        Parent(uuidConvertProto(packet.parent.parentuuid), packet.parent.parenthash.toByteArray())
    else
        null


    private fun <T> checkEmpty(value: T): T {
        if (empty) {
            throw IllegalStateException("empty newsgroup")
        }
        return value
    }

    var groupName: String = packet.name

    override fun hasKey(): UUID {
        return uuid
    }

    constructor(parcel: Parcel): this(
        groupName = parcel.readString()!!,
        description = parcel.readString()!!,
        uuid = parcel.readParcelable<ParcelUuid>(ParcelUuid::class.java.classLoader)!!.uuid,
        parentCol = parcel.readParcelable<ParcelUuid>(ParcelUuid::class.java.classLoader)!!.uuid,
        parentHash = parcel.createByteArray()!!,
    )

    constructor(
        uuid: UUID,
        parentCol: UUID? = null,
        groupName: String,
        description: String,
        parentHash: ByteArray? = null
    ): this(
        if (parentCol == null && parentHash == null)
            Subrosa.NewsGroup.newBuilder()
                .setToplevel(true)
                .setName(groupName)
                .setDescription(description)
                .setUuid(uuidConvertProto(uuid))
                .build()
        else if (parentCol != null && parentHash != null)
            Subrosa.NewsGroup.newBuilder()
                .setParent(
                    Subrosa.Parent.newBuilder()
                        .setParenthash(ByteString.copyFrom(parentHash))
                        .setParentuuid(uuidConvertProto(parentCol))
                        .build()
                )
                .setName(groupName)
                .setDescription(description)
                .setUuid(uuidConvertProto(uuid))
                .build()
        else
            throw IllegalArgumentException("parent hash nullablity must be the same"),
    )


    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(packet.name)
        dest.writeString(packet.description)
        dest.writeParcelable(ParcelUuid(uuidConvertProto(packet.uuid)), flags)
        if (packet.parentOptionCase == Subrosa.NewsGroup.ParentOptionCase.PARENT) {
            dest.writeParcelable(ParcelUuid(uuidConvertProto(packet.parent.parentuuid)), flags)
            dest.writeByteArray(packet.parent.parenthash.toByteArray())
        } else {
            dest.writeParcelable(ParcelUuid(uuidConvertProto(packet.uuid)), flags)
            dest.writeByteArray(ByteArray(0))
        }
    }



    companion object CREATOR : Parcelable.Creator<NewsGroup> {
        class Parser: Companion.Parser<Subrosa.NewsGroup, NewsGroup>(Subrosa.NewsGroup.parser()) {
            override val type: Subrosa.PostType = Subrosa.PostType.NEWSGROUP
        }
        val parser = Parser()

        override fun createFromParcel(parcel: Parcel): NewsGroup {
            return NewsGroup(parcel)
        }

        override fun newArray(size: Int): Array<NewsGroup?> {
            return arrayOfNulls(size)
        }

        fun fromPacket(packet: Subrosa.NewsGroup): NewsGroup {
            return NewsGroup(packet)
        }

        fun empty(): NewsGroup {
            val group =  NewsGroup(
                Subrosa.NewsGroup.newBuilder()
                    .build(),
            )
            group.empty = true
            return group
        }

    }
}