package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.domain.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.domain.customentities.util.resolveRequiredObjectReference
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.addError

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class ObjectTypeConfig(
	val objectReference: String,
	override val nullable: Boolean = false
) : GenericTypeConfig {
	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean =
		other is ObjectTypeConfig && (if (other.nullable == this.nullable) this == other else this == other.copy(nullable = this.nullable))

	@get:JsonIgnore
	override val objectDefinitionDependencies: Set<String> get() =
		setOf(objectReference)

	override fun validateConfig(
		context: CustomEntityConfigValidationContext,
	) {
		val definition = context.resolution.resolveObjectReference(objectReference)
		if (definition == null) {
			context.validation.addError("GE-OBJECT-MISSINGREF", "ref" to truncateValueForErrorMessage(objectReference))
		}
		// definition should have already been validated
	}

	override fun validateAndMapValueForStore(
		context: CustomEntityConfigValidationContext,
		value: RawJson,
	): RawJson = validatingNullForStore(context.validation, value, nullable) {
		if (value !is RawJson.JsonObject) {
			context.validation.addError("GE-OBJECT-JSON", "name" to truncateValueForErrorMessage(objectReference))
			value
		} else {
			context.resolution.resolveRequiredObjectReference(objectReference).validateAndMapExtensionValueForStore(
				context,
				value
			)
		}
	}
}