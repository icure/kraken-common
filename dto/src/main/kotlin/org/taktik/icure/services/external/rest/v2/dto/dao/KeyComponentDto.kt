package org.taktik.icure.services.external.rest.v2.dto.dao

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	property = "type",
)
@JsonSubTypes(
	value = [
		JsonSubTypes.Type(value = KeyComponentDto.IntKeyComponentDto::class, name = "int"),
		JsonSubTypes.Type(value = KeyComponentDto.FloatKeyComponentDto::class, name = "float"),
		JsonSubTypes.Type(value = KeyComponentDto.DoubleKeyComponentDto::class, name = "double"),
		JsonSubTypes.Type(value = KeyComponentDto.BooleanKeyComponentDto::class, name = "boolean"),
		JsonSubTypes.Type(value = KeyComponentDto.StringKeyComponentDto::class, name = "string"),
	],
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed interface KeyComponentDto {
	@ActiveField
	val value: Any?

	data class IntKeyComponentDto(
		override val value: Int?,
	) : KeyComponentDto

	data class FloatKeyComponentDto(
		override val value: Float?,
	) : KeyComponentDto

	data class DoubleKeyComponentDto(
		override val value: Double?,
	) : KeyComponentDto

	data class BooleanKeyComponentDto(
		override val value: Boolean?,
	) : KeyComponentDto

	data class StringKeyComponentDto(
		override val value: String?,
	) : KeyComponentDto
}
