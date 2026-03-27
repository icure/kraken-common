package org.taktik.icure.customentities.config.typing

import org.taktik.icure.jackson.annotations.JsonIgnore
import org.taktik.icure.jackson.annotations.JsonInclude
import org.taktik.icure.jackson.annotations.Include
import org.taktik.icure.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.addError
import org.taktik.icure.errorreporting.appending

@JsonInclude(Include.NON_DEFAULT)
data class MapTypeConfig(
	val valueType: GenericTypeConfig,
	override val nullable: Boolean = false,
	val validation: ValidationConfig? = null,
) : GenericTypeConfig {
	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean =
		other is MapTypeConfig && (if (other.nullable == this.nullable) this == other else this == other.copy(nullable = this.nullable))

	@get:JsonIgnore
	override val objectDefinitionDependencies: Set<Pair<String, Boolean>> get() =
		valueType.objectDefinitionDependencies + validation?.keyValidation?.equivalentTypeConfig()?.objectDefinitionDependencies.orEmpty()

	@get:JsonIgnore
	override val enumDefinitionDependencies: Set<Pair<String, Boolean>> get() =
		valueType.enumDefinitionDependencies + validation?.keyValidation?.equivalentTypeConfig()?.enumDefinitionDependencies.orEmpty()

	@JsonInclude(Include.NON_DEFAULT)
	data class ValidationConfig(
		val minSize: Int? = null,
		val maxSize: Int? = null,
		/**
		 * If set specifies which keys are allowed in the map.
		 * If null any string is allowed as key.
		 */
		val keyValidation: KeyValidation? = null,
	) {
		sealed interface KeyValidation {
			/**
			 * Get a [GenericTypeConfig] with validation rules equivalent to this key validation rules.
			 */
			fun equivalentTypeConfig(): GenericTypeConfig

			companion object {
				fun KeyValidation?.equivalentTypeConfig(): GenericTypeConfig = this?.equivalentTypeConfig() ?: StringTypeConfig(nullable = false)
			}

			/**
			 * Each key must be an entry of the referenced enum
			 */
			data class EnumKeyValidation(
				val enumReference: String
			) : KeyValidation {
				override fun equivalentTypeConfig(): GenericTypeConfig =
					EnumTypeConfig(enumReference = enumReference, isBuiltIn = false, nullable = false)
			}

			/**
			 * Each key must validate against the specified string validation configuration
			 */
			data class StringKeyValidation(
				val validation: StringTypeConfig.ValidationConfig,
			) : KeyValidation {
				override fun equivalentTypeConfig(): GenericTypeConfig =
					StringTypeConfig(nullable = false, validation = validation)
			}
		}
	}

	override fun validateConfig(
		context: CustomEntityConfigValidationContext,
	) {
		context.validation.appending("{*}") {
			valueType.validateConfig(context)
		}
		validation?.apply {
			if (minSize != null && minSize < 0) {
				context.validation.addError("GE-MAP-MIN")
			}
			if (maxSize != null && maxSize < 0) {
				context.validation.addError("GE-MAP-MAX")
			}
			if (minSize != null && maxSize != null && maxSize < minSize) {
				context.validation.addError("GE-MAP-NORANGE")
			} else if (maxSize == 0) {
				context.validation.addWarning("GE-MAP-WEMPTY")
			}
			if (minSize == 0) {
				context.validation.addWarning("GE-MAP-WMIN")
			}
			if (keyValidation != null) {
				context.validation.appending(".keyValidation") {
					keyValidation.equivalentTypeConfig().validateConfig(context)
				}
			}
		}
	}

	override fun validateAndMapValueForStore(
		context: CustomEntityConfigValidationContext,
		value: RawJson
	): RawJson = validatingNullForStore(context.validation, value, nullable) {
		if (value !is RawJson.JsonObject) {
			context.validation.addError("GE-MAP-JSON")
			value
		} else {
			val res =
				context.validation.appending("{") {
					value.properties.mapValues { (k, v) ->
						context.validation.appending(truncateValueForErrorMessage(k), "}") {
							valueType.validateAndMapValueForStore(
								context,
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
					context.validation.addError(
						"GE-MAP-OUTRANGE",
						"size" to res.size,
						"min" to (validation.minSize ?: "0"),
						"max" to (validation.maxSize ?: "*"),
					)
				}
				if (validation.keyValidation != null) {
					val equivalentTypeConfig = validation.keyValidation.equivalentTypeConfig()
					context.validation.appending("{KEY \"") {
						res.keys.forEach { key ->
							context.validation.appending(key, "\"}") {
								if (
									(equivalentTypeConfig.validateAndMapValueForStore(
										context,
										RawJson.JsonString(key)
									) as? RawJson.JsonString)?.value != key
								) {
									throw NotImplementedError("Internal error: support for map keys type that change during validation is not implemented")
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