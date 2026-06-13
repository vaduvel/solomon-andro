package ro.solomon.core.domain

import java.security.MessageDigest
import java.util.UUID

fun deterministicUUID(source: String): UUID {
    val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray())
    val bytes = digest.copyOfRange(0, 16)
    bytes[6] = (bytes[6].toInt() and 0x0F or 0x50).toByte()
    bytes[8] = (bytes[8].toInt() and 0x3F or 0x80).toByte()
    val msb = bytes.toLong(0)
    val lsb = bytes.toLong(8)
    return UUID(msb, lsb)
}

private fun ByteArray.toLong(start: Int): Long {
    var result = 0L
    for (i in 0 until 8) {
        result = result shl 8 or (this[start + i].toLong() and 0xFF)
    }
    return result
}
