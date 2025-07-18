package org.taktik.icure.utils

import java.lang.IllegalArgumentException

/**
 * Converts a String that represents bytes in their hexadecimal format to a ByteArray containing those bytes.
 * It will fail if the String has not the valid format.
 * @return a [ByteArray]
 */
fun String.hexStringToByteArray(): ByteArray {
	if (length % 2 != 0) {
		throw IllegalArgumentException("Invalid hexadecimal string")
	}
	val ret = ByteArray(length / 2)
	chunked(2).forEachIndexed { index, s ->
		ret[index] = s.toInt(16).toByte()
	}
	return ret
}
