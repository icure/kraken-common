package org.taktik.icure.domain.customentities.config.typing

import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath

data class EnumTypeConfig(
	val enumReference: String,
	val nullable: Boolean
) : GenericTypeConfig {
	override fun validateConfig(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath
	) {
		val definition = resolutionContext.resolveEnumReference(enumReference)
		requireNotNull(definition) {
			"$path: enum definition for reference `$enumReference` not found"
		}
		// definition should have already been validated
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: RawJson
	): RawJson = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is RawJson.JsonString) {
			"$path: invalid type, expected Text (enum)"
		}
		val enumDefinition = resolutionContext.resolveEnumReference(enumReference)!!
		require(value.value in enumDefinition.entries) {
			"$path: invalid value for enum $enumReference"
		}
		value
	}
}