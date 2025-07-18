/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.utils

import kotlinx.coroutines.delay
import java.math.BigInteger
import java.security.MessageDigest
import java.time.Duration
import java.util.UUID
fun <K> retry(trials: Int, closure: () -> K): K = retry(trials, closure) { true }

tailrec fun <K> retry(trials: Int, closure: () -> K, skipException: (e: Exception) -> Boolean): K {
	try {
		return closure()
	} catch (e: Exception) {
		if (trials < 1 || !skipException(e)) {
			throw e
		}
	}
	return retry(trials - 1, closure, skipException)
}

inline fun <K> retryIf(trials: Int, condition: (Exception) -> Boolean, closure: () -> K): K {
	require(trials > 0) { "trials must be > 0" }
	var remaining = trials
	while (true) {
		try {
			return closure()
		} catch (e: Exception) {
			remaining -= 1
			if (remaining <= 0 || !condition(e)) {
				throw e
			}
		}
	}
}
suspend inline fun <K, reified E> suspendRetryForSomeException(trials: Int, noinline closure: suspend () -> K): K = suspendRetry(trials, Duration.ZERO, closure) { it is E }

suspend fun <K> suspendRetry(trials: Int, closure: suspend () -> K): K = suspendRetry(trials, Duration.ZERO, closure)

suspend fun <K> suspendRetry(trials: Int, backOffDuration: Duration, closure: suspend () -> K) = suspendRetry(trials, backOffDuration, closure) { true }

tailrec suspend fun <K> suspendRetry(trials: Int, backOffDuration: Duration, closure: suspend () -> K, skipException: (e: Exception) -> Boolean): K {
	try {
		return closure()
	} catch (e: Exception) {
		if (trials < 1 || !skipException(e)) {
			throw e
		}
		val timeMillis = backOffDuration.toMillis()
		if (timeMillis > 0) {
			delay(timeMillis)
		}
	}
	return suspendRetry(trials - 1, backOffDuration.multipliedBy(2), closure, skipException)
}

/**
 * Converts the array into a string representation where each byte is represented by 2 hex digits.
 */
fun ByteArray.toHexString(): String = this.joinToString("") { it.toHexString() }

/**
 * Converts a byte into a hex string representation corresponding of exactly 2 characters.
 */
fun Byte.toHexString(): String = this.toInt().and(0xff).toString(16).padStart(2, '0')

fun UUID.xor(other: UUID): UUID = UUID(this.mostSignificantBits.xor(other.mostSignificantBits), this.leastSignificantBits.xor(other.leastSignificantBits))
