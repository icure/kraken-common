package org.taktik.icure.services.external.rest.v2.dto.utils

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JsonNode

@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	property = "type",
)
@JsonSubTypes(
	value = [
		JsonSubTypes.Type(value = ExternalFilterKeyDto.ExternalFilterStringKeyDto::class, name = "string"),
		JsonSubTypes.Type(value = ExternalFilterKeyDto.ExternalFilterLongKeyDto::class, name = "long"),
		JsonSubTypes.Type(value = ExternalFilterKeyDto.ExternalFilterComplexKeyDto::class, name = "complexKey"),
	],
)
sealed interface ExternalFilterKeyDto {
	val key: Any

	data class ExternalFilterStringKeyDto(
		override val key: String,
	) : ExternalFilterKeyDto

	data class ExternalFilterLongKeyDto(
		override val key: Long,
	) : ExternalFilterKeyDto

	data class ExternalFilterComplexKeyDto(
		override val key: JsonNode,
	) : ExternalFilterKeyDto
}
