package net.ballmerlabs.subrosa.scatterbrain

import net.ballmerlabs.subrosa.SubrosaProto
import java.util.*

fun uuidConvert(uuid: UUID): SubrosaProto.UUID {
    return SubrosaProto.UUID.newBuilder()
        .setLower(uuid.leastSignificantBits)
        .setUpper(uuid.mostSignificantBits)
        .build()
}

fun uuidConvert(uuid: SubrosaProto.UUID): UUID {
    return UUID(uuid.upper, uuid.lower)
}