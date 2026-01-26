package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.resolveRequiredEnumReference
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.appending

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
		validationContext: ScopedErrorCollector
	) {
		validationContext.appending("{*}") {
			valueType.validateConfig(
				resolutionContext,
				validationContext
			)
		}
		validation?.apply {
			if (minSize != null && minSize <= 0) {
				validationContext.addError("Invalid minSize, should be greater than 0")
			}
			if (maxSize != null && maxSize <= 0) {
				validationContext.addError("Invalid maxSize, should be greater than 0")
			}
			if (minSize != null && maxSize != null && maxSize < minSize) {
				validationContext.addError("Invalid size bounds, maxSize should be greater than or equal to minSize")
			}
			if (keyValidation != null) {
				validationContext.appending(".keyValidation") {
					when (keyValidation) {
						is ValidationConfig.KeyValidation.StringKeyValidation -> {
							keyValidation.validation.validateConfig(validationContext)
						}
						is ValidationConfig.KeyValidation.EnumKeyValidation -> {
							if (resolutionContext.resolveEnumReference(keyValidation.reference) == null) {
								validationContext.addError("Invalid enum reference")
							}
						}
					}
				}
			}
		}
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
		value: RawJson
	): RawJson = validatingAndIgnoringNullForStore(validationContext, value, nullable) {
		if (value !is RawJson.JsonObject) {
			validationContext.addError("Invalid type, expected Object (Map)")
			value
		} else {
			val res =
				validationContext.appending("{") {
					value.properties.mapValues { (k, v) ->
						validationContext.appending(truncateValueForErrorMessage(k), "}") {
							valueType.validateAndMapValueForStore(
								resolutionContext,
								validationContext,
								v
							)
						}
					}
				}
			if (validation != null) {
				if (
					(validation.minSize != null && res.size < validation.minSize)
						|| (validation.maxSize != null && res.size > validation.maxSize)
				) {
					validationContext.addError("Map size out of bounds")
				}
				if (validation.keyValidation != null) {
					validationContext.appending("{KEY \"") {
						when (validation.keyValidation) {
							is ValidationConfig.KeyValidation.StringKeyValidation -> {
								res.keys.forEach { key ->
									validationContext.appending(truncateValueForErrorMessage(key), "\"}") {
										validation.keyValidation.validation.validateValue(
											validationContext,
											key
										)
									}
								}
							}
							is ValidationConfig.KeyValidation.EnumKeyValidation -> {
								val enumDefinition = resolutionContext.resolveRequiredEnumReference(validation.keyValidation.reference)
								res.keys.forEach {
									if (it !in enumDefinition.entries) {
										validationContext.appending(truncateValueForErrorMessage(it), "\"}") {
											validationContext.addError("Invalid enum value, expected entry of enum ${validation.keyValidation.reference}")
										}
									}
								}
							}
						}
					}
				}
			}
			RawJson.JsonObject(res)
		}
	}
}