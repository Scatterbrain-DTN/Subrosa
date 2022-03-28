package net.ballmerlabs.subrosa.scatterbrain

import net.ballmerlabs.subrosa.SubrosaProto

enum class TypeVal {
    POST,
    NEWSGROUP,
    TYPE,
    INVALID,
    USER
}

fun toProto(typeVal: TypeVal): SubrosaProto.Type.PostType {
    return when(typeVal) {
        TypeVal.POST -> SubrosaProto.Type.PostType.POST
        TypeVal.NEWSGROUP -> SubrosaProto.Type.PostType.NEWSGROUP
        TypeVal.TYPE -> SubrosaProto.Type.PostType.TYPE
        TypeVal.USER -> SubrosaProto.Type.PostType.USER
        else -> SubrosaProto.Type.PostType.UNRECOGNIZED
    }
}

fun fromProto(type: SubrosaProto.Type.PostType): TypeVal {
    return when(type) {
        SubrosaProto.Type.PostType.NEWSGROUP -> TypeVal.NEWSGROUP
        SubrosaProto.Type.PostType.POST -> TypeVal.POST
        SubrosaProto.Type.PostType.TYPE -> TypeVal.TYPE
        SubrosaProto.Type.PostType.USER -> TypeVal.USER
        else -> TypeVal.INVALID
    }
}

class Type(packet: SubrosaProto.Type, val size: Int) : Message<SubrosaProto.Type>(packet) {
    val typeVal = fromProto(packet.type)

    override val typePacket: SubrosaProto.Type =  SubrosaProto.Type.newBuilder()
        .setType(toProto(TypeVal.TYPE))
        .build()

    companion object {
        class Parser: Message.Companion.Parser<SubrosaProto.Type, Type>(SubrosaProto.Type.parser()) {
            override val type: SubrosaProto.Type.PostType = SubrosaProto.Type.PostType.TYPE
        }
        val parser = Parser()
    }
}