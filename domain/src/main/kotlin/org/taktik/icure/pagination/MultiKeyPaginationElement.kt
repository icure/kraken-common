package org.taktik.icure.pagination

sealed interface MultiKeyPaginationElement<out T, out K> {
	data class Row<T>(val element: T) : MultiKeyPaginationElement<T, Nothing>
	data class NextPage<K>(val nextDocId: String? = null, val nextKeys: List<K>) : MultiKeyPaginationElement<Nothing, K>

	/**
	 * Signals that the flow stopped early because of [error], instead of because there was nothing left to return.
	 *
	 * The rows emitted before it are valid, but the page is incomplete and there is no cursor to resume from: without
	 * this element such a flow would be indistinguishable from a page that simply ended.
	 *
	 * It must follow at least one [Row] and should be the last element of the flow; anything the flow emits after it is
	 * ignored, since the page is already known to be incomplete.
	 */
	data class Aborted(val error: PaginationError) : MultiKeyPaginationElement<Nothing, Nothing>
}
