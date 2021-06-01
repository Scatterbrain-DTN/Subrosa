package net.ballmerlabs.subrosa.scatterbrain

import androidx.room.Ignore
import com.google.protobuf.MessageLite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ballmerlabs.subrosa.SubrosaProto
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val MASK = 0xFFFFFFFFL
private fun bytes2long(payload: ByteArray): Long {
    val buffer = ByteBuffer.wrap(payload)
    buffer.order(ByteOrder.BIG_ENDIAN)
    return (buffer.int.toLong() and MASK)
}

private fun longToByte(value: Long): ByteArray {
    val buffer = ByteBuffer.allocate(4)
    buffer.order(ByteOrder.BIG_ENDIAN)
    buffer.putInt(value.toInt())
    return buffer.array()
}

abstract class Message<T: MessageLite>(@Ignore val packet: T) {
    @Ignore
    val os = ByteArrayOutputStream()

    abstract val typePacket: SubrosaProto.Type

    val bytes: ByteArray
    get() {
        val typeLen = typePacket.serializedSize.toLong()
        val packetLen = packet.serializedSize.toLong()
        val out = ByteBuffer.allocate((Int.SIZE_BYTES*2 + typeLen + packetLen).toInt())
        out.put(longToByte(typeLen))
        out.put(typePacket.toByteArray())
        out.put(longToByte(packetLen))
        out.put(packet.toByteArray())
        return out.array()
    }

    fun writeToStream(os: OutputStream) {
        packet.writeDelimitedTo(os)
    }

    companion object {
        abstract class Parser<T: MessageLite, V: Message<T>>(
            val parser: com.google.protobuf.Parser<T>
        ) {
            abstract val type: SubrosaProto.Type.PostType
        }

        inline fun <reified T: Message<V>, reified V: MessageLite> parse(
            parser: Parser<V,T>,
            input: ByteArray
        ): T {
            val message = parser.parser.parseFrom(input)
            return T::class.java.getConstructor(V::class.java).newInstance(message)
        }

        // blocking function are ok here because we run with IO dispatcher
        @Suppress("BlockingMethodInNonBlockingContext")
        suspend fun parse(input: ByteArray): Type {
            val s = ByteBuffer.wrap(input, 0, Int.SIZE_BYTES).order(ByteOrder.BIG_ENDIAN).int
            val typeBuf = ByteBuffer.wrap(input, Int.SIZE_BYTES, s)
            return withContext(Dispatchers.IO) { Type(Type.parser.parser.parseFrom(typeBuf)) }
        }

        // blocking function are ok here because we run with IO dispatcher
        @Suppress("BlockingMethodInNonBlockingContext")
        suspend inline fun <reified T> parse(
            bytes: ByteArray,
            type: Type
        ): T {
            val rs = ByteBuffer.wrap(bytes, type.packet.serializedSize, Int.SIZE_BYTES).order(ByteOrder.BIG_ENDIAN).int
            val rbuf = ByteBuffer.wrap(bytes, type.packet.serializedSize + Int.SIZE_BYTES, rs)
            return withContext(Dispatchers.IO) {
                when (type.typeVal) {
                    TypeVal.POST -> parse(Post.parser, rbuf.array()) as T
                    TypeVal.NEWSGROUP -> parse(NewsGroup.parser, rbuf.array()) as T
                    else -> throw IllegalStateException("invalid type: ${type.typeVal}")
                }
            }
        }

    }
}