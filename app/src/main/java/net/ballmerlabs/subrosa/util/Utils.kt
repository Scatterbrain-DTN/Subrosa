package net.ballmerlabs.subrosa.util

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import net.ballmerlabs.subrosa.SubrosaProto
import java.nio.ByteBuffer
import java.security.MessageDigest
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


fun uuidConvertProto(uuid: UUID): SubrosaProto.UUID {
    return SubrosaProto.UUID.newBuilder()
        .setLower(uuid.leastSignificantBits)
        .setUpper(uuid.mostSignificantBits)
        .build()
}

fun uuidConvertProto(uuid: SubrosaProto.UUID): UUID {
    return UUID(uuid.upper, uuid.lower)
}

suspend fun checkPermission(permission: String, context: Context): Boolean = suspendCancellableCoroutine { c ->
    if (ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        c.resumeWith(Result.success(true))
    } else {

        c.resumeWith(Result.success(false))
    }

}

fun Int.toDp(): Int = (this / Resources.getSystem().displayMetrics.density).toInt()
fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

/**
 * NOTE: this is NOT cryptographically secure, probably,
 */
fun uuidSha256(bytes: ByteArray): UUID {
    val buf = ByteBuffer.wrap(
        MessageDigest.getInstance("SHA-256").digest(bytes)
    )
    return UUID(buf.long, buf.long)
}