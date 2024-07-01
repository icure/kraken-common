package org.taktik.icure.services.external.rest.v2.dto.utils


import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JsonNode

@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	property = "type"
)
@JsonSubTypes(value = [
	JsonSubTypes.Type(value = ExternalFilterKeyDto.ExternalFilterStringKeyDto::class, name = "string"),
	JsonSubTypes.Type(value = ExternalFilterKeyDto.ExternalFilterLongKeyDto::class, name = "long"),
	JsonSubTypes.Type(value = ExternalFilterKeyDto.ExternalFilterComplexKeyDto::class, name = "complexKey"),
])
sealed interface ExternalFilterKeyDto<T> {

	val key: T

	data class ExternalFilterStringKeyDto(override val key: String) : ExternalFilterKeyDto<String>
	data class ExternalFilterLongKeyDto(override val key: Long) : ExternalFilterKeyDto<Long>
	data class ExternalFilterComplexKeyDto(override val key: JsonNode) : ExternalFilterKeyDto<JsonNode>
}
