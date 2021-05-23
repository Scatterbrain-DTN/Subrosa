package net.ballmerlabs.subrosa.scatterbrain

import androidx.room.Ignore
import com.google.protobuf.ByteString
import com.google.protobuf.MessageLite
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

abstract class Message<T: MessageLite>(@Ignore val packet: T) {
    @Ignore
    val os = ByteArrayOutputStream()

    @Ignore
    val bytes: ByteArray = try {
                packet.writeDelimitedTo(os)
                os.toByteArray()
            } catch (e: IOException) {
                byteArrayOf(0) //this should be unreachable
            }

    @Ignore
    val byteString = ByteString.copyFrom(bytes)

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