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
	companion object {
		/**
		 * Biggest long that we can safely store without incurring in any issues with js numbers or couchdb views
		 */
		private const val MAX_SAFE_LONG = 9007199254740991
		/**
		 * Smallest long that we can safely store without incurring in any issues with js numbers or couchdb views
		 */
		private const val MIN_SAFE_LONG = -9007199254740991
	}

	data class ValidationConfig(
		/**
		 * Minimum acceptable value for the , always inclusive
		 */
		val min: Long? = null,
		/**
		 * Maximum acceptable value for the , always inclusive
		 */
		val max: Long? = null,
	)

	override fun validateConfig(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
	) {
		validation?.apply {
			require(min != null || max != null) {
				"$path: invalid int validation config, must specify a lower and/or upper limit"
			}
			require(min == null || min in MIN_SAFE_LONG..MAX_SAFE_LONG) {
				"$path: invalid int validation config, value for `min` is not within the safe range for JS integers"
			}
			require(max == null || max in MIN_SAFE_LONG..MAX_SAFE_LONG) {
				"$path: invalid int validation config, value for `max` is not within the safe range for JS integers"
			}
			require(max == null || min == null || max > min) {
				"$path: invalid int validation config, value for `max` should be greater than value for `min`"
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
		val valueLong = value.asLong()
		require (
			valueLong >= (validation?.min ?: MIN_SAFE_LONG) && valueLong <= (validation?.max ?: MAX_SAFE_LONG)
		) {
			"$path: value $valueLong out of configured bounds"
		}
		value
	}
}