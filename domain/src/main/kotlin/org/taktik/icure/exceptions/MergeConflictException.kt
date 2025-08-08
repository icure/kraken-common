package org.taktik.icure.exceptions

import java.lang.IllegalArgumentException

/**
 * An exception used to indicate that the automatic merging of an entity could not be performed and requires human
 * intervention.
 */
class MergeConflictException(
	message: String,
	cause: Throwable? = null,
) : IllegalArgumentException(message, cause)
