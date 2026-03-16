package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.domain.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.domain.customentities.util.resolveRequiredEnumReference
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.addError

/**
 * A reference to a custom enum definition
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class EnumTypeConfig(
	val enumReference: String,
	val isBuiltIn: Boolean = false,
	override val nullable: Boolean = false
) : GenericTypeConfig {
	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean =
		other is EnumTypeConfig && (if (other.nullable == this.nullable) this == other else this == other.copy(nullable = this.nullable))

	@get:JsonIgnore
	override val enumDefinitionDependencies: Set<String> get() =
		setOf(enumReference)

	override fun validateConfig(
		context: CustomEntityConfigValidationContext,
	) {
		if (isBuiltIn) TODO("validate built-in enum reference")
		val definition = context.resolution.resolveEnumReference(enumReference)
		if (definition == null) {
			context.validation.addError(
				"GE-ENUM-MISSINGREF",
				"ref" to enumReference
			)
		}
		// definition should have already been validated
	}

	override fun validateAndMapValueForStore(
		context: CustomEntityConfigValidationContext,
		value: RawJson
	): RawJson = validatingNullForStore(context.validation, value, nullable) {
		if (isBuiltIn) TODO("validate built-in enum reference")
		if (value !is RawJson.JsonString) {
			context.validation.addError("GE-ENUM-JSON")
		} else {
			val enumDefinition = context.resolution.resolveRequiredEnumReference(enumReference)
			if (value.value !in enumDefinition.entries) {
				context.validation.addError(
					"GE-ENUM-VALUE",
					"value" to truncateValueForErrorMessage(value.value),
					"ref" to truncateValueForErrorMessage(enumReference),
				)
			}
		}
		value
	}
}