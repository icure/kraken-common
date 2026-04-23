package org.taktik.icure.customentities.config.typing

import org.taktik.icure.jackson.annotations.JsonIgnore
import org.taktik.icure.jackson.annotations.JsonInclude
import org.taktik.icure.jackson.annotations.JsonIncludeValue
import org.taktik.icure.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.customentities.util.CustomEntityValueValidationContext
import org.taktik.icure.customentities.util.resolveRequiredEnumReference
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.addError

/**
 * A reference to a custom enum definition
 */
@JsonInclude(JsonIncludeValue.NON_DEFAULT)
data class EnumTypeConfig(
	val enumReference: String,
	val isBuiltin: Boolean = false,
	override val nullable: Boolean = false
) : GenericTypeConfig {
	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean =
		other is EnumTypeConfig && (if (other.nullable == this.nullable) this == other else this == other.copy(nullable = this.nullable))

	@get:JsonIgnore
	override val enumDefinitionDependencies: Set<Pair<String, Boolean>> get() =
		setOf(Pair(enumReference, isBuiltin))

	override fun validateConfig(
		context: CustomEntityConfigValidationContext,
	) {
		val definition = if (isBuiltin) {
			context.validation.addError("GE-BUILTIN-IN-CUSTOM")
			context.builtinDefinitions.getBuiltinEnumDefinition(enumReference)
		} else {
			context.resolution.resolveEnumReference(enumReference)
		}
		if (definition == null) {
			context.validation.addError(
				"GE-ENUM-MISSINGREF",
				"ref" to truncateValueForErrorMessage(enumReference),
				"builtin" to isBuiltin
			)
		}
		// definition should have already been validated
	}

	override fun validateAndMapValueForStore(
		context: CustomEntityValueValidationContext,
		value: RawJson
	): RawJson = validatingNullForStore(context.validation, value, nullable) {
		if (value !is RawJson.JsonString) {
			context.validation.addError("GE-ENUM-JSON")
			value
		} else if (isBuiltin) {
			throw UnsupportedOperationException("Builtin enum in custom extension or custom object is not currently supported")
			// context.builtinValidation.validateAndMapBuiltinEnum(enumReference, value, context.validation)
		} else {
			if (value.value !in context.resolution.resolveRequiredEnumReference(enumReference).entries) {
				context.validation.addError(
					"GE-ENUM-VALUE",
					"value" to truncateValueForErrorMessage(value.value),
					"ref" to truncateValueForErrorMessage(enumReference),
				)
			}
			value
		}
	}
}