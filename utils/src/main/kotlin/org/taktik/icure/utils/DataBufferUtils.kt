package org.taktik.icure.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import java.nio.ByteBuffer

/**
 * Creates a new byte array from the data buffer, then releases the data buffer if requested.
 */
fun DataBuffer.toByteArray(thenRelease: Boolean): ByteArray = ByteArray(readableByteCount()).also {
	read(it)
	if (thenRelease) DataBufferUtils.release(this)
}

fun ByteBuffer.toDataBuffer(): DataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(array())

fun Flow<ByteBuffer>.asDataBuffer() = this.map { it.toDataBuffer() }
