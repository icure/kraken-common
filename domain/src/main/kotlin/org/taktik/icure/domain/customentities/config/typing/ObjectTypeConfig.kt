package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath
import org.taktik.icure.domain.customentities.util.resolveRequiredObjectReference

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class ObjectTypeConfig(
	val objectReference: String,
	val nullable: Boolean = false
) : GenericTypeConfig {
	override fun validateConfig(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath
	) {
		val definition = resolutionContext.resolveObjectReference(objectReference)
		requireNotNull(definition) {
			"$path: object definition for reference `$objectReference` not found"
		}
		// definition should have already been validated
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: RawJson
	): RawJson = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is RawJson.JsonObject) {
			"$path: invalid type, expected Object ($objectReference)"
		}
		resolutionContext.resolveRequiredObjectReference(objectReference).validateAndMapValueForStore(
			resolutionContext,
			path,
			value
		)
	}
}