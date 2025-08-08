package org.taktik.icure.pagination

import org.taktik.couchdb.id.Identifiable

/**
 * This is a marker interface for the elements that are part of a paginated list and that can be included in a
 * [PaginatedFlux]. The only allowed elements are:
 * - The elements of the row array.
 * - The next page, if there are more elements to be emitted.
 * It was decided to remove the total size, as it cannot be accurate, especially after filtering.
 */
sealed interface PaginationElement

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
