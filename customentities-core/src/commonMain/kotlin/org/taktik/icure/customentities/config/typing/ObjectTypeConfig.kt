package org.taktik.icure.customentities.config.typing

import org.taktik.icure.jackson.annotations.JsonIgnore
import org.taktik.icure.jackson.annotations.JsonInclude
import org.taktik.icure.jackson.annotations.JsonIncludeValue
import org.taktik.icure.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.customentities.util.CustomEntityValueValidationContext
import org.taktik.icure.customentities.util.resolveRequiredObjectReference
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.addError

@JsonInclude(JsonIncludeValue.NON_DEFAULT)
data class ObjectTypeConfig(
	val objectReference: String,
	override val nullable: Boolean = false,
	val isBuiltin: Boolean = false,
) : GenericTypeConfig {
	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean =
		other is ObjectTypeConfig && (if (other.nullable == this.nullable) this == other else this == other.copy(nullable = this.nullable))

	@get:JsonIgnore
	override val objectDefinitionDependencies: Set<Pair<String, Boolean>> get() =
		setOf(Pair(objectReference, isBuiltin))

	override fun validateConfig(
		context: CustomEntityConfigValidationContext,
	) {
		val definition = if (isBuiltin) {
			context.validation.addError("GE-BUILTIN-IN-CUSTOM")
			context.builtinDefinitions.getBuiltinObjectDefinition(objectReference)
		} else {
			val customObjectDefinition = context.resolution.resolveObjectReference(objectReference)
			if (customObjectDefinition?.builtinExtension != null) {
				context.validation.addError("GE-BUILTIN-IN-CUSTOM")
			}
			customObjectDefinition
		}
		if (definition == null) {
			context.validation.addError(
				"GE-OBJECT-MISSINGREF",
				"ref" to truncateValueForErrorMessage(objectReference),
				"builtin" to isBuiltin
			)
		}
		// definition should have already been validated
	}

	override fun validateAndMapValueForStore(
		context: CustomEntityValueValidationContext,
		value: RawJson,
	): RawJson = validatingNullForStore(context.validation, value, nullable) {
		if (value !is RawJson.JsonObject) {
			context.validation.addError("GE-OBJECT-JSON", "name" to truncateValueForErrorMessage(objectReference))
			value
		} else if (isBuiltin) {
			throw UnsupportedOperationException("Builtin enum in custom extension or custom object is not currently supported")
			// context.builtinValidation.validateAndMapPlainBuiltinObject(objectReference, value, context.validation)
		} else {
			context.resolution.resolveRequiredObjectReference(objectReference).validateAndMapValueForStore(
				context,
				value
			)
		}
	}
}