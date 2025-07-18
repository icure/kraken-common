package org.taktik.icure.services.external.rest.v2.dto

data class ListOfPropertiesDto(
	val properties: Set<PropertyStubDto> = emptySet(),
)
