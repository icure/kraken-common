package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonIgnore
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath

class ObjectTypeConfig(
	val objectReference: String,
	val nullable: Boolean
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
		resolutionContext.resolveObjectReference(objectReference)!!.validateAndMapValueForStore(
			resolutionContext,
			path,
			value
		)
	}

	override fun mapValueForRead(
		resolutionContext: CustomEntityConfigResolutionContext,
		value: RawJson
	): RawJson =
		if (value is RawJson.JsonObject) {
			resolutionContext.resolveObjectReference(objectReference)!!.mapValueForRead(resolutionContext, value)
		} else {
			value
		}

	override val shouldMapForRead: Boolean @JsonIgnore get() = true
}