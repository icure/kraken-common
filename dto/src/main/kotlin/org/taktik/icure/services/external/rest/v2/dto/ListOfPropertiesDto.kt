package org.taktik.icure.services.external.rest.v2.dto

/**
 * DTO wrapping a set of property stubs, used for bulk property operations.
 */
data class ListOfPropertiesDto(
	/** The set of property stubs. */
	val properties: Set<PropertyStubDto> = emptySet(),
)
