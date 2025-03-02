package net.ballmerlabs.subrosa.scatterbrain

import subrosaproto.Subrosa

enum class TypeVal {
    POST,
    NEWSGROUP,
    TYPE,
    INVALID,
    USER
}

fun toProto(typeVal: TypeVal): Subrosa.PostType {
    return when(typeVal) {
        TypeVal.POST -> Subrosa.PostType.POST
        TypeVal.NEWSGROUP -> Subrosa.PostType.NEWSGROUP
        TypeVal.TYPE -> Subrosa.PostType.TYPE
        TypeVal.USER -> Subrosa.PostType.USER
        else -> Subrosa.PostType.UNRECOGNIZED
    }
}

fun fromProto(type: Subrosa.PostType): TypeVal {
    return when(type) {
        Subrosa.PostType.NEWSGROUP -> TypeVal.NEWSGROUP
        Subrosa.PostType.POST -> TypeVal.POST
        Subrosa.PostType.TYPE -> TypeVal.TYPE
        Subrosa.PostType.USER -> TypeVal.USER
        else -> TypeVal.INVALID
    }
}

class Type(packet: Subrosa.TypePrefix, val size: Int) : Message<Subrosa.TypePrefix>(packet) {
    val typeVal = fromProto(packet.postType)

    override val typePacket: Subrosa.TypePrefix =  Subrosa.TypePrefix.newBuilder()
        .setPostType(toProto(TypeVal.TYPE))
        .build()

    companion object {
        class Parser: Message.Companion.Parser<Subrosa.TypePrefix, Type>(Subrosa.TypePrefix.parser()) {
            override val type: Subrosa.PostType = Subrosa.PostType.TYPE
        }
        val parser = Parser()
    }
}