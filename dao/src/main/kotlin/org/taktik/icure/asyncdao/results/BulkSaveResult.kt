package org.taktik.icure.asyncdao.results

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.taktik.couchdb.BulkUpdateResult
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.exceptions.ConflictRequestException

sealed interface BulkSaveResult<out T> {
    /**
     * Get the entity if the save was successful, else throw an exception.
     * The thrown exception will cause the appropriate response to the end user on the GlobalErrorHandler.
     */
    fun entityOrThrow(): T

    data class Success<T>(val entity: T): BulkSaveResult<T> {
        override fun entityOrThrow(): T = entity
    }

    data class Failure(
        // Should be same as equivalent http code
        val code: Int,
        val message: String,
        val entityId: String
    ): BulkSaveResult<Nothing> {
        override fun entityOrThrow(): Nothing = when (code) {
            409 -> throw ConflictRequestException(message)
            // 403 means kraken has wrong credentials, not that user provided wrong credentials: also mapped as IllegalState
            else -> throw IllegalStateException("Could not save entity $entityId during bulk update. Code: $code. Reason: $message")
        }
    }
}

fun <T : Any> BulkSaveResult<T>.entityOrNull(): T? = when (this) {
    is BulkSaveResult.Failure -> null
    is BulkSaveResult.Success -> entity
}

fun <T> Flow<BulkSaveResult<T>>.filterSuccessfulUpdates(): Flow<T> =
    mapNotNull { (it as? BulkSaveResult.Success<T>)?.entity }

/**
 * Converts a [BulkUpdateResult] to a [BulkSaveResult.Failure] if the result is an error, else returns null.
 */
fun BulkUpdateResult.toBulkSaveResultFailure(): BulkSaveResult.Failure? = when (error) {
    // Error types from couchdb doc @https://docs.couchdb.org/en/stable/api/database/bulk-api.html#bulk-document-validation-and-conflict-errors
    null -> null
    "conflict" -> BulkSaveResult.Failure(409, "Conflict: $reason", id)
    "forbidden" -> BulkSaveResult.Failure(403, "Forbidden: $reason", id)
    else -> BulkSaveResult.Failure(500, "Unknown error: $error. Reason: $reason", id)
}