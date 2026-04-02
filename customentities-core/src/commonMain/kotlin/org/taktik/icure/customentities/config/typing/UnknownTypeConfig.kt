package org.taktik.icure.customentities.config.typing

import org.taktik.icure.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.entities.RawJson
import org.taktik.icure.jackson.annotations.JsonIncludeValue
import org.taktik.icure.jackson.annotations.JsonInclude

/**
 * A type configuration used to represent a value with unknown validation rules.
 *
 * There is no value valid for this type, and it is not possible to migrate to/from an unknown value.
 *
 * This configuration is used only to support builtin entities definitions and should not be used by custom entities
 * and custom extensions on builtin entities.
 */
@JsonInclude(JsonIncludeValue.NON_DEFAULT)
class UnknownTypeConfig private constructor (override val nullable: Boolean = false) : GenericTypeConfig {
	companion object {
		fun of(nullable: Boolean): UnknownTypeConfig =
			if (nullable) Nullable else NonNull

		val NonNull = UnknownTypeConfig(false)
		val Nullable = UnknownTypeConfig(true);
	}

	override fun validateAndMapValueForStore(
		context: CustomEntityConfigValidationContext,
		value: RawJson
	): RawJson {
		context.validation.addError("GE-UNKNOWN-VALIDATE")
		return value
	}

	// For purpose of validation we always consider two unknown types to never be equal
	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean =
		false
}