package net.ballmerlabs.subrosa.util

import net.ballmerlabs.subrosa.SubrosaProto
import java.nio.ByteBuffer
import java.util.*

fun uuidConvert(uuid: UUID): ByteArray {
    val buf = ByteBuffer.wrap(ByteArray(16))
    buf.putLong(uuid.mostSignificantBits)
    buf.putLong(uuid.leastSignificantBits)
    return buf.array()
}

fun uuidConvert(uuid: SubrosaProto.UUID): ByteArray {
    val buf = ByteBuffer.wrap(ByteArray(16))
    buf.putLong(uuid.upper)
    buf.putLong(uuid.lower)
    return buf.array()
}

fun uuidConvert(bytes: ByteArray): UUID {
    val buf = ByteBuffer.wrap(bytes)
    val high: Long = buf.long
    val low: Long = buf.long
    return UUID(high, low)
}