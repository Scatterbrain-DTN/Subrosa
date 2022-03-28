package net.ballmerlabs.subrosa.scatterbrain

import android.util.Log
import androidx.room.Ignore
import com.google.protobuf.MessageLite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ballmerlabs.subrosa.SubrosaProto
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

private fun longToByte(value: Int): ByteArray {
    val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
    buffer.order(ByteOrder.BIG_ENDIAN)
    buffer.putInt(value)
    return buffer.array()
}

abstract class Message<T: MessageLite>(@Ignore val packet: T) {
    @Ignore
    val os = ByteArrayOutputStream()

    abstract val typePacket: SubrosaProto.Type

    val bytes: ByteArray
    get() {
        val typeArray = typePacket.toByteArray()
        val packetArray = packet.toByteArray()
        val typeLen = typeArray.size
        val packetLen = packetArray.size
        Log.e("debug", "sent typeLen $typeLen packetLen $packetLen")
        val out = ByteBuffer.allocate(Int.SIZE_BYTES*2 + typeLen + packetLen)
        out.put(longToByte(typeLen))
        out.put(typeArray)
        out.put(longToByte(packetLen))
        out.put(packetArray)
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
            input: ByteArray,
            offset: Int,
            len: Int
        ): T {
            val message = parser.parser.parseFrom(input, offset, len)
            return T::class.java.getConstructor(V::class.java).newInstance(message)
        }

        // blocking function are ok here because we run with IO dispatcher
        @Suppress("BlockingMethodInNonBlockingContext")
        suspend fun parse(input: ByteArray): Type {
            val s = ByteBuffer.wrap(input, 0, Int.SIZE_BYTES).order(ByteOrder.BIG_ENDIAN).int
            Log.e("debug", "parsed typeLen $s")
            return withContext(Dispatchers.IO) { Type(Type.parser.parser.parseFrom(input, Int.SIZE_BYTES, s), s) }
        }

        // blocking function are ok here because we run with IO dispatcher
        @Suppress("BlockingMethodInNonBlockingContext")
        suspend inline fun <reified T> parse(
            bytes: ByteArray,
            type: Type
        ): T {
            val sizeOffset = type.size + Int.SIZE_BYTES
            val messageOffset = sizeOffset + Int.SIZE_BYTES
            val rs = ByteBuffer.wrap(bytes, sizeOffset, Int.SIZE_BYTES).order(ByteOrder.BIG_ENDIAN).int
            Log.e("debug", "parsed packetLen $rs")
            return withContext(Dispatchers.IO) {
                when (type.typeVal) {
                    TypeVal.POST -> parse(Post.parser, bytes, messageOffset, rs) as T
                    TypeVal.NEWSGROUP -> parse(NewsGroup.parser, bytes, messageOffset, rs) as T
                    TypeVal.USER -> parse(User.parser, bytes, messageOffset, rs) as T
                    else -> throw IllegalStateException("invalid type: ${type.typeVal}")
                }
            }
        }

    }
}