package org.taktik.icure.customentities.config.typing

import org.taktik.icure.jackson.annotations.JsonInclude
import org.taktik.icure.jackson.annotations.Include
import org.taktik.icure.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.entities.RawJson

/**
 * Represents a configuration for a boolean type.
 */
@JsonInclude(Include.NON_DEFAULT)
data class BooleanTypeConfig(
	override val nullable: Boolean = false,
) : GenericTypeConfig {
	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean =
		other is BooleanTypeConfig && (if (other.nullable == this.nullable) this == other else this == other.copy(nullable = this.nullable))

	override fun validateAndMapValueForStore(
		context: CustomEntityConfigValidationContext,
		value: RawJson
	): RawJson =
		validatingNullForStore(
			context.validation,
			value,
			nullable
		) {
			if (value !is RawJson.JsonBoolean) context.validation.addError("GE-BOOL-JSON")
			value
		}
}