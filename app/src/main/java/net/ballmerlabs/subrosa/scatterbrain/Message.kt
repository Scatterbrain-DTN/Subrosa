package net.ballmerlabs.subrosa.scatterbrain

import androidx.room.Ignore
import com.google.protobuf.CodedInputStream
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
import java.util.zip.CRC32

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

        suspend fun parsePrefixType(bytes: ByteArray) {
            val type = parse(Type.parser, bytes)
            when(type.typeVal) {
                TypeVal.POST -> parse(Post.parser, bytes)
                TypeVal.NEWSGROUP -> parse(NewsGroup.parser, bytes)
                else -> throw IllegalStateException("invalid type: ${type.typeVal}")
            }
        }

        // blocking function are ok here because we run with IO dispatcher
        @Suppress("BlockingMethodInNonBlockingContext")
        suspend inline fun <reified T: Message<V>, reified V: MessageLite> parse(
            parser: Parser<V,T>,
            input: ByteArray
        ): T = withContext(Dispatchers.IO) {
            val message = parser.parser.parseFrom(input)
            T::class.java.getConstructor(V::class.java).newInstance(message)
        }

        fun <T : MessageLite> parseFromCRC(parser: com.google.protobuf.Parser<T>, inputStream: InputStream): T {
            val crc = ByteArray(4)
            val size = ByteArray(4)
            if (inputStream.read(size) != 4) {
                throw IOException("end of stream")
            }
            val s = ByteBuffer.wrap(size).order(ByteOrder.BIG_ENDIAN).int
            val co = CodedInputStream.newInstance(inputStream, s + 1)
            val messageBytes = co.readRawBytes(s)
            val message = parser.parseFrom(messageBytes)
            if (inputStream.read(crc) != crc.size) {
                throw IOException("end of stream")
            }
            val crc32 = CRC32()
            crc32.update(messageBytes)
            if (crc32.value != bytes2long(crc)) {
                throw IOException("invalid crc: " + crc32.value + " " + bytes2long(crc))
            }
            return message
        }

    }
}