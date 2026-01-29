package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.resolveRequiredEnumReference
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.addError

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class EnumTypeConfig(
	val enumReference: String,
	val nullable: Boolean = false
) : GenericTypeConfig {
	override fun validateConfig(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
	) {
		val definition = resolutionContext.resolveEnumReference(enumReference)
		if (definition == null) {
			validationContext.addError(
				"GE-ENUM-MISSINGREF",
				"ref" to enumReference
			)
		}
		// definition should have already been validated
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
		value: RawJson
	): RawJson = validatingAndIgnoringNullForStore(validationContext, value, nullable) {
		if (value !is RawJson.JsonString) {
			validationContext.addError("GE-ENUM-JSON")
		} else {
			val enumDefinition = resolutionContext.resolveRequiredEnumReference(enumReference)
			if (value.value !in enumDefinition.entries) {
				validationContext.addError(
					"GE-ENUM-VALUE",
					"value" to truncateValueForErrorMessage(value.value),
					"ref" to truncateValueForErrorMessage(enumReference),
				)
			}
		}
		value
	}
}