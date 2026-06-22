package org.taktik.icure.services.external.rest.v2.dto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * DTO wrapping a set of property stubs, used for bulk property operations.
 */
data class ListOfPropertiesDto(
	/** The set of property stubs. */
	@ActiveField val properties: Set<PropertyStubDto> = emptySet(),
)
