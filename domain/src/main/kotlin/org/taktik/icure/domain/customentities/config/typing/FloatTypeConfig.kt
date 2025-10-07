package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.FloatNode
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath

data class FloatTypeConfig(
	val nullable: Boolean = false,
	val validation: ValidationConfig? = null
) : GenericTypeConfig {
	data class ValidationConfig(
		val min: Double? = null,
		val max: Double? = null,
		val exclusiveMin: Boolean = false,
		val exclusiveMax: Boolean = false
	)

	override fun validateConfig(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
	) {
		validation?.apply {
			require(min != null || max != null) {
				"$path: invalid float validation config, must specify a lower and/or upper limit"
			}
		}
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: JsonNode
	): JsonNode = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is DoubleNode || value is FloatNode) {
			"$path: invalid type, expected Float64"
		}
		val valueDouble = value.asDouble()
		require (valueDouble.isFinite()) {
			"$path: value $valueDouble out of bounds for Float64"
		}
		if (validation != null) {
			require (
				validation.min?.let { min -> if (validation.exclusiveMin) valueDouble > min else valueDouble >= min } ?: true
					&& validation.max?.let { max -> if (validation.exclusiveMax) valueDouble < max else valueDouble <= max } ?: true
			) {
				"$path: value $valueDouble out of configured bounds"
			}
		}
		value
	}
}
