package org.taktik.icure.pagination

sealed interface MultiKeyPaginationElement<out T, out K> {
	data class Row<T>(val element: T) : MultiKeyPaginationElement<T, Nothing>
	data class NextPage<K>(val nextDocId: String? = null, val nextKeys: List<K>) : MultiKeyPaginationElement<Nothing, K>
}