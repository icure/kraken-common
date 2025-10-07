package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
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
		value: JsonNode
	): JsonNode = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is ObjectNode) {
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
		value: JsonNode
	): JsonNode =
		if (value is ObjectNode) {
			resolutionContext.resolveObjectReference(objectReference)!!.mapValueForRead(resolutionContext, value)
		} else {
			value
		}

	override val shouldMapForRead: Boolean get() = true
}