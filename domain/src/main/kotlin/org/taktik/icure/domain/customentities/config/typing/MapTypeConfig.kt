package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
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
		/**
		 * If set specifies which keys are allowed in the map.
		 * If null any string is allowed as key.
		 */
		val keyValidation: KeyValidation? = null,
	) {
		@JsonTypeInfo(
			use = JsonTypeInfo.Id.NAME,
			property = "type",
		)
		@JsonSubTypes(
			JsonSubTypes.Type(value = KeyValidation.EnumKeyValidation::class, name = "Enum"),
			JsonSubTypes.Type(value = KeyValidation.StringKeyValidation::class, name = "String"),
		)
		sealed interface KeyValidation {
			/**
			 * Each key must be an entry of the referenced enum
			 */
			data class EnumKeyValidation(
				val reference: String
			) : KeyValidation

			/**
			 * Each key must validate against the specified string validation configuration
			 */
			data class StringKeyValidation(
				val validation: StringTypeConfig.ValidationConfig,
			) : KeyValidation
		}
	}

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
			when (keyValidation) {
				null -> {} // nothing to do
				is ValidationConfig.KeyValidation.StringKeyValidation -> {
					path.appending(".keyValidation") {
						keyValidation.validation.validateConfig(path)
					}
				}
				is ValidationConfig.KeyValidation.EnumKeyValidation -> {
					requireNotNull(resolutionContext.resolveEnumReference(keyValidation.reference)) {
						"$path.keyValidation: invalid enum reference"
					}
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
			path.appending("{", truncateValueForErrorMessage(k), "}") {
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
			when (val keyValidation = validation.keyValidation) {
				is ValidationConfig.KeyValidation.StringKeyValidation -> {
					res.keys.forEach { key ->
						path.appending("{KEY \"", truncateValueForErrorMessage(key), "\"}") {
							keyValidation.validation.validateValue(
								path,
								key
							)
						}
					}
				}
				is ValidationConfig.KeyValidation.EnumKeyValidation -> {
					val enumDefinition = resolutionContext.resolveEnumReference(keyValidation.reference)!!
					res.keys.forEach {
						require(it in enumDefinition.entries) {
							"$path{KEY \"${truncateValueForErrorMessage(it)}\"}: expected entry of enum ${keyValidation.reference}"
						}
					}
				}
				null -> {} // nothing to do
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