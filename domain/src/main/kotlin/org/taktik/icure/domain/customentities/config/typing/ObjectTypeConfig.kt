package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.resolveRequiredObjectReference
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.addError

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class ObjectTypeConfig(
	val objectReference: String,
	override val nullable: Boolean = false
) : GenericTypeConfig {
	override val objectDefinitionDependencies: Set<String> get() =
		setOf(objectReference)

	override fun validateConfig(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
	) {
		val definition = resolutionContext.resolveObjectReference(objectReference)
		if (definition == null) {
			validationContext.addError("GE-OBJECT-MISSINGREF", "ref" to truncateValueForErrorMessage(objectReference))
		}
		// definition should have already been validated
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
		value: RawJson,
	): RawJson = validatingNullForStore(validationContext, value, nullable) {
		if (value !is RawJson.JsonObject) {
			validationContext.addError("GE-OBJECT-JSON", "name" to truncateValueForErrorMessage(objectReference))
			value
		} else {
			resolutionContext.resolveRequiredObjectReference(objectReference).validateAndMapValueForStore(
				resolutionContext,
				validationContext,
				value
			)
		}
	}
}