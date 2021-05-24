package net.ballmerlabs.subrosa.scatterbrain

import androidx.room.Ignore
import com.google.protobuf.MessageLite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ballmerlabs.subrosa.SubrosaProto
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

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

    abstract val type: TypeVal

    @Ignore
    val typePacket: SubrosaProto.Type = SubrosaProto.Type.newBuilder()
        .setType(toProto(type))
        .build()

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
        suspend fun parsePrefixType(inputStream: InputStream) {
            val type = parse(Type.parser, inputStream)
            when(type.type) {
                TypeVal.POST -> parse(Post.parser, inputStream)
                TypeVal.NEWSGROUP -> parse(NewsGroup.parser, inputStream)
                else -> throw IllegalStateException("invalid type: ${type.type}")
            }
        }

        // blocking function are ok here because we run with IO dispatcher
        @Suppress("BlockingMethodInNonBlockingContext")
        suspend inline fun <reified T: Message<V>, reified V: MessageLite> parse(
            parser: Parser<V,T>,
            inputStream: InputStream
        ): T = withContext(Dispatchers.IO) {
            val message = parser.parser.parseDelimitedFrom(inputStream)
            T::class.java.getConstructor(V::class.java).newInstance(message)
        }
    }
}