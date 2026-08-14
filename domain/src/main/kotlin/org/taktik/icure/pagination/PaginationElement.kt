package org.taktik.icure.pagination

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.couchdb.id.Identifiable

/**
 * This is a marker interface for the elements that are part of a paginated list and that can be included in a
 * [PaginatedFlux]. The only allowed elements are:
 * - The elements of the row array.
 * - The next page, if there are more elements to be emitted.
 * - The error that aborted the page, if it could not be returned in full.
 * It was decided to remove the total size, as it cannot be accurate, especially after filtering.
 */
sealed interface PaginationElement

/**
 * The error that aborted a paginated flow, for the cases where the request can't simply be failed: the response was
 * already committed with a success status and some rows were already emitted, so the failure has to be part of the body
 * instead of the http status.
 *
 * The fields mirror the error body that the global error handler produces for a failed request, with the addition of
 * [statusCode], which for a normal error is carried by the status of the response instead.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaginationError(
	/**
	 * The http status code that the request would have failed with, had nothing been emitted yet.
	 */
	val statusCode: Int,
	/**
	 * A human readable description of what went wrong, as it would have been returned in the body of a failed request.
	 */
	val message: String,
	/**
	 * A machine readable discriminator for the error, for the errors that define one; null otherwise.
	 */
	val exceptionDetail: String? = null,
)

/**
 * A wrapper for the actual element of type [T] to be added to the row array of the paginated list.
 * It also contains the key [K] of the element in the view.
 */
data class PaginationRowElement<T, K>(
	val element: T,
	val key: K? = null,
) : PaginationElement {

	fun asNextPageElement(): NextPageElement<K> = when {
		element is Identifiable<*> && element.id is String -> NextPageElement(element.id as String, key)
		element is String -> NextPageElement(element, key)
		else -> throw IllegalArgumentException("Cannot derive a NextPage element from this entity")
	}
}

/**
 * Represents the key to retrieve the next page. If present, it must be the last element of the flow.
 */
data class NextPageElement<K>(val startKeyDocId: String? = null, val startKey: K? = null) : PaginationElement

/**
 * Signals that the flow stopped early because of [error], instead of because there was nothing left to return.
 *
 * The rows emitted before it are valid, but the page is incomplete and there is no cursor to resume from: without this
 * element such a flow would be indistinguishable from a page that simply ended.
 *
 * It must follow at least one [PaginationRowElement] and should be the last element of the flow; anything the flow
 * emits after it is ignored, since the page is already known to be incomplete.
 */
data class AbortedPageElement(val error: PaginationError) : PaginationElement
