package org.taktik.icure.utils

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import kotlin.reflect.KClass

/**
 * Mask all the exceptions to a [ResponseStatusException] with a [HttpStatus.BAD_REQUEST] status, except for the ones
 * passed as parameters.
 *
 * @param e an [Exception].
 * @param notMapped the [KClass] of all the exceptions that are not to be masked.
 * @return the [Exception] to throw.
 */
fun maskToBadRequestExceptFor(
	e: Exception,
	vararg notMapped: KClass<*>,
): Exception = notMapped
	.firstOrNull {
		e::class == it
	}?.let { e } ?: ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
