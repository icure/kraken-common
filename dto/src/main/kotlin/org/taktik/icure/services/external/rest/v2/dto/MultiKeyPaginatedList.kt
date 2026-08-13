package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.dto.annotations.filtering.ActiveField
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Generic DTO representing a paginated list of results for a search that paginates over multiple view keys at once.
 * Contains the current page of rows and, if there is more, the cursor for fetching the next page.
 *
 * @param T The type of elements in the paginated list.
 * @param K The type of the keys the search paginates over.
 */
data class MultiKeyPaginatedList<T, K>(
	/** The list of results for the current page. */
	@ActiveField val rows: List<T> = emptyList(),
	/** The cursor to use for fetching the next page of results, or null if this is the last page. */
	@ActiveField val nextPage: NextPage<K>? = null,
) : Serializable {
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	/**
	 * The cursor for the next page of a multi-key paginated search: both values must be passed back to the same
	 * search, together with the search parameters of the original request.
	 */
	data class NextPage<K>(
		/** The document identifier to start the next page from. */
		@ActiveField val nextDocId: String? = null,
		/**
		 * The keys to search for in the next page: the first one is the only key that may have been partially
		 * returned, and all the keys that were already fully returned have been dropped.
		 */
		@ActiveField val nextKeys: List<K> = emptyList(),
	) : Serializable
}
