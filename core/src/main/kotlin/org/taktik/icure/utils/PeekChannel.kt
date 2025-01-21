package org.taktik.icure.utils

import kotlinx.coroutines.channels.Channel


// Note: not thread safe for read
class PeekChannel<T : Any>(capacity: Int) {
	private val source = Channel<T>(capacity)
	private var peeked: T? = null
	private var closedRead: Boolean = false

	suspend fun peekOrNull(): T? {
		return when {
			peeked != null -> peeked
			closedRead -> null
			else -> {
				val next = source.receiveCatching()
				if (next.isClosed) {
					closedRead = true
					null
				} else {
					next.getOrThrow().also {
						peeked = it
					}
				}
			}
		}
	}

	suspend fun consume() {
		peeked = null
	}

	suspend fun send(e: T) {
		source.send(e)
	}

	fun closeSend() {
		source.close()
	}
}