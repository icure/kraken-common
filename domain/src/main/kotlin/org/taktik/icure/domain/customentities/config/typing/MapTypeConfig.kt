package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class MapTypeConfig(
	val valueType: GenericTypeConfig,
	val nullable: Boolean = false,
	val validation: ValidationConfig? = null,
) : GenericTypeConfig {
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	data class ValidationConfig(
		val minSize: Int? = null,
		val maxSize: Int? = null,
		val keyValidationEnumReference: String? = null,
	)

	override fun validateConfig(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath
	) {
		path.appending("{x}") {
			valueType.validateConfig(
				resolutionContext,
				path
			)
		}
		validation?.apply {
			require(minSize == null || minSize > 0) {
				"$path: invalid minSize, should be greater than 0"
			}
			require(maxSize == null || maxSize > 0) {
				"$path: invalid maxSize, should be greater than 0"
			}
			require(minSize == null || maxSize == null || maxSize >= minSize) {
				"$path: invalid size bounds, maxSize should be greater than or equal to minSize"
			}
			if (keyValidationEnumReference != null) {
				requireNotNull(resolutionContext.resolveEnumReference(keyValidationEnumReference)) {
					"$path: invalid enum reference"
				}
			}
		}
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: RawJson
	): RawJson = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is RawJson.JsonObject) {
			"$path: invalid type, expected Object (Map)"
		}
		val res = value.properties.mapValues { (k, v) ->
			path.appending("{", k, "}") {
				valueType.validateAndMapValueForStore(
					resolutionContext,
					path,
					v
				)
			}
		}
		if (validation != null) {
			require(
				(validation.minSize == null || res.size >= validation.minSize)
					&& (validation.maxSize == null || res.size <= validation.maxSize)
			) {
				"$path: map size out of bounds"
			}
			if (validation.keyValidationEnumReference != null) {
				val enumDefinition = resolutionContext.resolveEnumReference(validation.keyValidationEnumReference)!!
				res.keys.forEach {
					require(it in enumDefinition.entries) {
						"$path: invalid key value, expected entry of enum ${validation.keyValidationEnumReference}"
					}
				}
			}
		}
		RawJson.JsonObject(res)
	}

	override fun mapValueForRead(
		resolutionContext: CustomEntityConfigResolutionContext,
		value: RawJson
	): RawJson = if (valueType.shouldMapForRead && value is RawJson.JsonObject) {
		RawJson.JsonObject(
			value.properties.mapValues { (_, v) ->
				valueType.mapValueForRead(
					resolutionContext,
					v
				)
			}
		)
	} else {
		value
	}

	override val shouldMapForRead: Boolean @JsonIgnore get() = true
}