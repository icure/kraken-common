package org.taktik.icure.customentities.config.typing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.taktik.icure.jackson.annotations.JsonIgnore
import org.taktik.icure.jackson.annotations.JsonInclude
import org.taktik.icure.jackson.annotations.JsonIncludeValue
import org.taktik.icure.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.customentities.util.CustomEntityValueValidationContext
import org.taktik.icure.customentities.util.resolveRequiredObjectReference
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.addError

@JsonInclude(JsonIncludeValue.NON_DEFAULT)
@SerialName("Object")
@Serializable
data class ObjectTypeConfig(
	val objectReference: String,
	override val nullable: Boolean = false,
	val isBuiltin: Boolean = false,
) : GenericTypeConfig {
	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean =
		other is ObjectTypeConfig && (if (other.nullable == this.nullable) this == other else this == other.copy(nullable = this.nullable))

	override fun areEquivalent(a: RawJson, b: RawJson, resolutionContext: CustomEntityConfigResolutionContext?): Boolean {
		if (a == b) return true
		if (a !is RawJson.JsonObject || b !is RawJson.JsonObject) return false
		val definition = resolutionContext?.resolveObjectReference(objectReference) ?: return false
		return definition.areEquivalent(a, b, resolutionContext)
	}

	@get:JsonIgnore
	override val objectDefinitionDependencies: Set<Pair<String, Boolean>> get() =
		setOf(Pair(objectReference, isBuiltin))

	override fun validateConfig(
		context: CustomEntityConfigValidationContext,
	) {
		val definition = if (isBuiltin) {
			context.validation.addError("GED-BUILTIN-IN-CUSTOM")
			context.builtinDefinitions.getBuiltinObjectDefinition(objectReference)
		} else {
			val customObjectDefinition = context.resolution.resolveObjectReference(objectReference)
			if (customObjectDefinition?.builtinExtension != null) {
				context.validation.addError("GED-BUILTIN-IN-CUSTOM")
			}
			customObjectDefinition
		}
		if (definition == null) {
			context.validation.addError(
				"GED-OBJECT-MISSINGREF",
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
			context.validation.addError("GEV-OBJECT-JSON", "name" to truncateValueForErrorMessage(objectReference))
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