package org.taktik.icure.services.external.rest.v2.dto

/**
 * DTO wrapping a list of identifier-revision pairs, used for bulk operations on versioned entities.
 */
data class ListOfIdsAndRevDto(
	/** The list of identifier-revision pairs. */
	val ids: List<IdWithRevDto>,
)
