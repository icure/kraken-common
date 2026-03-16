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
/**
 * Sealed interface representing a typed filter key used when constructing external filter queries.
 * Implementations cover string, long, and complex (JSON node) key types.
 */
sealed interface ExternalFilterKeyDto {
	val key: Any

	/**
	 * A filter key backed by a string value.
	 */
	data class ExternalFilterStringKeyDto(
		/** The string key value. */
		override val key: String,
	) : ExternalFilterKeyDto

	/**
	 * A filter key backed by a long integer value.
	 */
	data class ExternalFilterLongKeyDto(
		/** The long key value. */
		override val key: Long,
	) : ExternalFilterKeyDto

	/**
	 * A filter key backed by an arbitrary JSON node, suitable for composite or structured keys.
	 */
	data class ExternalFilterComplexKeyDto(
		/** The JSON node representing the complex key value. */
		override val key: JsonNode,
	) : ExternalFilterKeyDto
}
