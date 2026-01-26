package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.resolveRequiredObjectReference
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopedErrorCollector

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class ObjectTypeConfig(
	val objectReference: String,
	val nullable: Boolean = false
) : GenericTypeConfig {
	override fun validateConfig(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
	) {
		val definition = resolutionContext.resolveObjectReference(objectReference)
		if (definition == null) {
			validationContext.addError("Object definition for reference `$objectReference` not found")
		}
		// definition should have already been validated
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
		value: RawJson,
	): RawJson = validatingAndIgnoringNullForStore(validationContext, value, nullable) {
		if (value !is RawJson.JsonObject) {
			validationContext.addError("Invalid type, expected Object ($objectReference)")
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