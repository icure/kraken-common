package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.LongNode
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath


data class IntTypeConfig(
	val nullable: Boolean = false,
	val validation: ValidationConfig? = null
) : GenericTypeConfig {

	data class ValidationConfig(
		val min: Long? = null,
		val max: Long? = null,
		val exclusiveMin: Boolean = false,
		val exclusiveMax: Boolean = false
	)

	override fun validateConfig(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
	) {
		validation?.apply {
			require(min != null || max != null) {
				"$path: invalid int validation config, must specify a lower and/or upper limit"
			}
		}
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: JsonNode
	): JsonNode = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is IntNode || value is LongNode) {
			"$path: invalid type, expected Int64"
		}
		if (validation != null) {
			val valueLong = value.asLong()
			require (
				validation.min?.let { min -> if (validation.exclusiveMin) valueLong > min else valueLong >= min } ?: true
					&& validation.max?.let { max -> if (validation.exclusiveMax) valueLong < max else valueLong <= max } ?: true
			) {
				"$path: value $valueLong out of configured bounds"
			}
		}
		value
	}
}