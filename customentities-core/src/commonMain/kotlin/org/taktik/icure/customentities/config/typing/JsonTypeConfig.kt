package org.taktik.icure.customentities.config.typing

import org.taktik.icure.jackson.annotations.JsonIgnore
import org.taktik.icure.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.entities.RawJson

/**
 * A type representing some unstructured json that should be stored without any validation.
 * This can be any json string, boolean, object, array, or null.
 * This type configuration, however, has stricter limitations for json numbers:
 * - Integer numbers that are too large to be represented as a 64-bit signed integer will be rejected.
 * - Floating point numbers that are too large to be represented as a finite 64-bit floating point number will be rejected.
 * - Floating point numbers within the limit of 64-bit floating point representation will be accepted, but may lose
 * precision or change representation when stored, as described in [FloatTypeConfig].
 */
data object JsonTypeConfig : GenericTypeConfig {
	override fun validateAndMapValueForStore(
		context: CustomEntityConfigValidationContext,
		value: RawJson
	): RawJson =
		value

	@get:JsonIgnore
	override val nullable: Boolean
		get() = true

	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean =
		other == JsonTypeConfig
}