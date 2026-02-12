package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.resolveRequiredEnumReference
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.addError
import org.taktik.icure.errorreporting.appending

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class MapTypeConfig(
	val valueType: GenericTypeConfig,
	override val nullable: Boolean = false,
	val validation: ValidationConfig? = null,
) : GenericTypeConfig {
	override val objectDefinitionDependencies: Set<String> get() =
		valueType.objectDefinitionDependencies

	override val enumDefinitionDependencies: Set<String> get() =
		valueType.enumDefinitionDependencies + setOfNotNull(validation?.keyValidation?.let {
			when (it) {
				is ValidationConfig.KeyValidation.EnumKeyValidation -> it.reference
				else -> null
			}
		})

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
			if (minSize != null && minSize < 0) {
				validationContext.addError("GE-MAP-MIN")
			}
			if (maxSize != null && maxSize < 0) {
				validationContext.addError("GE-MAP-MAX")
			}
			if (minSize != null && maxSize != null && maxSize < minSize) {
				validationContext.addError("GE-MAP-NORANGE")
			} else if (maxSize == 0) {
				validationContext.addWarning("GE-MAP-WEMPTY")
			}
			if (minSize == 0) {
				validationContext.addWarning("GE-MAP-WMIN")
			}
			if (keyValidation != null) {
				validationContext.appending(".keyValidation") {
					when (keyValidation) {
						is ValidationConfig.KeyValidation.StringKeyValidation -> {
							keyValidation.validation.validateConfig(validationContext)
						}
						is ValidationConfig.KeyValidation.EnumKeyValidation -> {
							if (resolutionContext.resolveEnumReference(keyValidation.reference) == null) {
								validationContext.addError(
									"GE-MAP-KEYENUM-REF",
									"ref" to keyValidation.reference
								)
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
	): RawJson = validatingNullForStore(validationContext, value, nullable) {
		if (value !is RawJson.JsonObject) {
			validationContext.addError("GE-MAP-JSON")
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
					validationContext.addError(
						"GE-MAP-OUTRANGE",
						"size" to res.size,
						"min" to (validation.minSize ?: "0"),
						"max" to (validation.maxSize ?: "*"),
					)
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
											validationContext.addError(
												"GE-MAP-KEYENUM-VALUE",
												"key" to it,
												"ref" to validation.keyValidation.reference
											)
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