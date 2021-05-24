package net.ballmerlabs.subrosa.scatterbrain

import net.ballmerlabs.subrosa.SubrosaProto

enum class TypeVal {
    POST,
    NEWSGROUP,
    TYPE,
    INVALID
}

fun toProto(typeVal: TypeVal): SubrosaProto.Type.PostType {
    return when(typeVal) {
        TypeVal.POST -> SubrosaProto.Type.PostType.POST
        TypeVal.NEWSGROUP -> SubrosaProto.Type.PostType.NEWSGROUP
        TypeVal.TYPE -> SubrosaProto.Type.PostType.TYPE
        else -> SubrosaProto.Type.PostType.UNRECOGNIZED
    }
}

fun fromProto(type: SubrosaProto.Type.PostType): TypeVal {
    return when(type) {
        SubrosaProto.Type.PostType.NEWSGROUP -> TypeVal.NEWSGROUP
        SubrosaProto.Type.PostType.POST -> TypeVal.POST
        SubrosaProto.Type.PostType.TYPE -> TypeVal.TYPE
        else -> TypeVal.INVALID
    }
}

class Type(packet: SubrosaProto.Type) : Message<SubrosaProto.Type>(packet) {
    val typeVal = fromProto(packet.type)

    override val typePacket: SubrosaProto.Type =  SubrosaProto.Type.newBuilder()
        .setType(toProto(TypeVal.TYPE))
        .build()

    constructor(typeVal: TypeVal): this(
        SubrosaProto.Type.newBuilder()
            .setType(toProto(typeVal))
            .build()
    )

    companion object {
        class Parser: Message.Companion.Parser<SubrosaProto.Type, Type>(SubrosaProto.Type.parser()) {
            override val type: SubrosaProto.Type.PostType = SubrosaProto.Type.PostType.TYPE
        }
        val parser = Parser()
    }
}