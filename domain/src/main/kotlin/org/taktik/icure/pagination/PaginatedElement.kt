package org.taktik.icure.pagination

/**
 * This is a marker interface for the elements that are part of a paginated list and that can be included in a
 * [PaginatedFlux]. The only allowed elements are:
 * - The elements of the row array.
 * - The next page, if there are more elements to be emitted.
 * It was decided to remove the total size, as it cannot be accurate, especially after filtering.
 */
sealed interface PaginatedElement

/**
 * A wrapper for the actual element of type [T] to be added to the row array of the paginated list.
 */
@JvmInline
value class PaginatedRowElement<T>(val element: T) : PaginatedElement

/**
 * Represents the key to retrieve the next page. If present, it must be the last element of the flow.
 */
data class NextPageElement<K>(val startKeyDocId: String? = null, val startKey: K? = null): PaginatedElement