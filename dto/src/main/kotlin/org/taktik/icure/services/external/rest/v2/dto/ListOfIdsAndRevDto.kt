package org.taktik.icure.services.external.rest.v2.dto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * DTO wrapping a list of identifier-revision pairs, used for bulk operations on versioned entities.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.ListOfIdsAndRevDto")
data class ListOfIdsAndRevDto(
	/** The list of identifier-revision pairs. */
	@ActiveField val ids: List<IdWithRevDto>,
)
