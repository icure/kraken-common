package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.resolveRequiredEnumReference
import org.taktik.icure.errorreporting.ScopedErrorCollector

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
		if (definition == null) validationContext.addError(
			"Enum definition for reference `$enumReference` not found"
		)
		// definition should have already been validated
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
		value: RawJson
	): RawJson = validatingAndIgnoringNullForStore(validationContext, value, nullable) {
		if (value !is RawJson.JsonString) {
			validationContext.addError("Invalid type, expected Text (enum)")
		} else {
			val enumDefinition = resolutionContext.resolveRequiredEnumReference(enumReference)
			if (value.value !in enumDefinition.entries) validationContext.addError("Invalid value for enum $enumReference")
		}
		value
	}
}