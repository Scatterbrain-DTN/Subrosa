package net.ballmerlabs.subrosa.scatterbrain

import com.google.protobuf.ByteString
import com.google.protobuf.MessageLite
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

abstract class Message<T: MessageLite>(val packet: T) {
    var luid: UUID? = null

    val bytes: ByteArray
        get() {
            val os = ByteArrayOutputStream()
            return try {
                packet.writeDelimitedTo(os)
                os.toByteArray()
            } catch (e: IOException) {
                byteArrayOf(0) //this should be unreachable
            }
        }

    val byteString: ByteString
        get() = ByteString.copyFrom(bytes)

    fun writeToStream(os: OutputStream) {
        packet.writeDelimitedTo(os)
    }

    companion object {
        abstract class Parser<T: MessageLite, V: Message<T>>(
            val parser: com.google.protobuf.Parser<T>
        )

        inline fun <reified T: Message<V>, reified V: MessageLite> parse(
            parser: Parser<V,T>,
            inputStream: InputStream
        ): T {
            val message = parser.parser.parseDelimitedFrom(inputStream)
            return T::class.java.getConstructor(V::class.java).newInstance(message)
        }
    }
}