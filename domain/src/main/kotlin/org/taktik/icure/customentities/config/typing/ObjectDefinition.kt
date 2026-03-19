package org.taktik.icure.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.taktik.icure.customentities.config.ExtendableEntityName
import org.taktik.icure.entities.RawJson
import org.taktik.icure.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.errorreporting.addError
import org.taktik.icure.errorreporting.addWarning
import org.taktik.icure.errorreporting.appending
import org.taktik.icure.utils.FuzzyDates
import org.taktik.icure.utils.Validation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * A definition of a custom or built-in object type
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class ObjectDefinition(
	/**
	 * The "data" properties of the object.
	 * These are properties that have only the purpose of storing data and do not have a specific meaning for the system,
	 * as opposed to metadata properties, such as:
	 * - id
	 * - revision
	 * - creation, modification, deletion timestamps
	 * - security metadata, legacy delegations, other access control related metadata
	 * - attachment metadata
	 * - versioning metadata (e.g. healthElementId and endOfLife on health elements)
	 *
	 * If this object definition extends a builtin entity, these properties are in addition to the ones already defined
	 * in the builtin entity.
	 * It is possible, though discouraged, to use the same name as a property already defined in the builtin entity; in
	 * that case the SDK generator will move the standard entity property value in to a different property.
	 *
	 * Should be not empty if [baseEntity] is null.
	 */
	val properties: Map<String, PropertyConfiguration> = emptyMap(),
	/**
	 * If this configuration extends a builtin entity the standard entity being extended; if null this configuration
	 * does not extend any builtin entity.
	 */
	val baseEntity: ExtendableEntityName? = null,
	/**
	 * A map to configure extension on builtin properties of the extended builtin entity, if any.
	 *
	 * The key must be the name of a property with an extendable type defined in the entity corresponding to this
	 * definition [baseEntity].
	 *
	 * The value is a reference to an object definition that specifies extension properties for the type of the
	 * specified property.
	 *
	 * This can also be applied if the extendable type is nested within a collection or in a map's value, even if
	 * there are multiple levels of nesting.
	 */
	val extendedBuiltinProperties: Map<String, String> = emptyMap(),
) {
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	data class PropertyConfiguration(
		val type: GenericTypeConfig,
		val defaultValue: DefaultValue? = null
	) {
		/**
		 * If a property defines a default value and is not explicitly provided in the object it is implied that it
		 * should use the default value
		 */
		@JsonTypeInfo(
			use = JsonTypeInfo.Id.NAME,
			property = "type",
		)
		@JsonSubTypes(
			JsonSubTypes.Type(value = DefaultValue.Constant::class, name = "Constant"),
			JsonSubTypes.Type(value = DefaultValue.GenerateUuidV4::class, name = "GenerateUuidV4"),
			JsonSubTypes.Type(value = DefaultValue.NowDateTime::class, name = "NowDateTime"),
			JsonSubTypes.Type(value = DefaultValue.NowDate::class, name = "NowDate"),
			JsonSubTypes.Type(value = DefaultValue.NowTime::class, name = "NowTime"),
		)
		sealed interface DefaultValue {
			/**
			 * Checks if this default value is valid for the configured property
			 */
			suspend fun validateFor(
				typeConfig: GenericTypeConfig,
				context: CustomEntityConfigValidationContext
			)

			/**
			 * Get a value matching this default configuration; if not null the value must be stored in the DB.
			 * May change at each invocation
			 */
			fun valueForStore(): RawJson?

			/**
			 * If the provided value should be ignored according to this default configuration.
			 */
			fun shouldIgnoreForStore(value: RawJson): Boolean

			@get:JsonIgnore
			val isConstant: Boolean

			/**
			 * Represents a constant default value.
			 * Properties with values equal to the default value are not included in the serialized representation
			 */
			@JsonInclude(JsonInclude.Include.NON_DEFAULT)
			data class Constant(
				/**
				 * The default value
				 */
				@param:JsonInclude(JsonInclude.Include.ALWAYS)
				val value: RawJson,
			) : DefaultValue {
				override suspend fun validateFor(
					typeConfig: GenericTypeConfig,
					context: CustomEntityConfigValidationContext,
				) {
					// Technically supported on all types even though doesn't really make sense to have a "constant" id or date
					// For simplicity will probably just hide it on the frontend
					typeConfig.validateAndMapValueForStore(context, value)
				}

				override fun valueForStore(): RawJson? = null

				override fun shouldIgnoreForStore(value: RawJson): Boolean =
					value == this.value

				@get:JsonIgnore
				override val isConstant: Boolean = true
			}

			data object GenerateUuidV4 : DefaultValue {
				override suspend fun validateFor(
					typeConfig: GenericTypeConfig,
					context: CustomEntityConfigValidationContext,
				) {
					if (
						typeConfig !is UuidTypeConfig
					) {
						context.validation.addError("GE-OBJECT-DEFAULT-UUIDV4TYPE")
					}
				}

				override fun valueForStore(): RawJson =
					RawJson.JsonString(UUID.randomUUID().toString())

				override fun shouldIgnoreForStore(value: RawJson): Boolean =
					false

				@get:JsonIgnore
				override val isConstant: Boolean = false
			}

			@JsonInclude(JsonInclude.Include.NON_DEFAULT)
			class NowDateTime(
				val zoneId: String? = null
			) : DefaultValue {
				override suspend fun validateFor(
					typeConfig: GenericTypeConfig,
					context: CustomEntityConfigValidationContext,
				) {
					if (
						typeConfig !is FuzzyDateTimeTypeConfig
					) {
						context.validation.addError("GE-OBJECT-DEFAULT-NOWDATETIMETYPE")
					}
					zoneId?.let {
						if (!Validation.validZoneId(it)) {
							context.validation.addError("GE-ZONEID",  "value" to it)
						}
					}
				}

				override fun valueForStore(): RawJson =
					RawJson.JsonInteger(
						FuzzyDates.getFuzzyDateTime(
							LocalDateTime.now(zoneId?.let { ZoneId.of(it) } ?: ZoneOffset.UTC),
							ChronoUnit.SECONDS,
							false
						)
					)

				override fun shouldIgnoreForStore(value: RawJson): Boolean =
					false

				@get:JsonIgnore
				override val isConstant: Boolean = false
			}

			@JsonInclude(JsonInclude.Include.NON_DEFAULT)
			class NowDate(
				val zoneId: String? = null
			) : DefaultValue {
				override suspend fun validateFor(
					typeConfig: GenericTypeConfig,
					context: CustomEntityConfigValidationContext,
				) {
					if (
						typeConfig !is FuzzyDateTypeConfig
					) {
						context.validation.addError("GE-OBJECT-DEFAULT-NOWDATETYPE")
					}
					zoneId?.let {
						if (!Validation.validZoneId(it)) {
							context.validation.addError("GE-ZONEID", "value" to it)
						}
					}
				}

				override fun valueForStore(): RawJson =
					RawJson.JsonInteger(
						FuzzyDates.getFuzzyDate(
							LocalDate.now(zoneId?.let { ZoneId.of(it) } ?: ZoneOffset.UTC),
							ChronoUnit.DAYS,
							false
						).toLong()
					)

				override fun shouldIgnoreForStore(value: RawJson): Boolean =
					false

				override val isConstant: Boolean = false
			}

			@JsonInclude(JsonInclude.Include.NON_DEFAULT)
			class NowTime(
				val zoneId: String? = null
			) : DefaultValue {
				override suspend fun validateFor(
					typeConfig: GenericTypeConfig,
					context: CustomEntityConfigValidationContext,
				) {
					if (
						typeConfig !is FuzzyTimeTypeConfig
					) {
						context.validation.addError("GE-OBJECT-DEFAULT-NOWTIMETYPE")
					}
					zoneId?.let {
						if (!Validation.validZoneId(it)) {
							context.validation.addError("GE-ZONEID", "value" to it)
						}
					}
				}

				override fun valueForStore(): RawJson =
					RawJson.JsonInteger(
						FuzzyDates.getFuzzyTime(
							LocalTime.now(zoneId?.let { ZoneId.of(it) } ?: ZoneOffset.UTC),
						).toLong()
					)

				override fun shouldIgnoreForStore(value: RawJson): Boolean =
					false

				override val isConstant: Boolean = false
			}
		}
	}

	suspend fun validateDefinition(
		context: CustomEntityConfigValidationContext,
	) {
		if (properties.isEmpty() && baseEntity == null) {
			context.validation.addWarning("GE-OBJECT-WEMPTY")
		}
		context.validation.appending(".") {
			properties.forEach { (propName, propConfig) ->
				context.validation.appending(propName) {
					validateIdentifier(context.validation, propName)
					propConfig.type.validateConfig(context)
					propConfig.type.objectDefinitionDependencies.forEach { definitionName ->
						context.resolution.resolveObjectReference(definitionName)?.also { definition ->
							if (definition.baseEntity?.isRootEntity == true) {
								context.validation.addError("GE-OBJECT-EMBEDROOT", "object" to definitionName, "entity" to definition.baseEntity)
							}
						}
					}
					context.validation.appending("<DEFAULT>") {
						propConfig.defaultValue?.validateFor(propConfig.type, context)
					}
				}
			}
		}
		if (baseEntity != null) {
			val builtinProperties = context.builtinDefinitions.getBuiltinObjectDefinition(baseEntity)
			if (builtinProperties == null) {
				context.validation.addError("GE-OBJECT-BASEENTITYREF", "entity" to baseEntity)
			} else {
				context.validation.appending(".") {
					properties.keys.intersect(builtinProperties.keys).forEach {
						context.validation.appending(it) {
							// Probably no real implementation complexity or other limitation in allowing it, but root
							// entities also include access control and other metadata that does not work when embedded
							context.validation.addWarning("GE-OBJECT-WBASEENTITYPROP", "prop" to it, "entity" to baseEntity)
						}
					}
				}
				extendedBuiltinProperties.forEach { builtinPropName, targetDefinition ->
					TODO("Validate extended builtin prop $builtinPropName with definition $targetDefinition")
				}
			}
		} else {
			if (extendedBuiltinProperties.isNotEmpty()) {
				TODO("ERROR")
			}
		}
	}

	fun validateAndMapValueForStore(
		context: CustomEntityConfigValidationContext,
		value: RawJson.JsonObject,
	): RawJson =
		if (baseEntity != null) {
			context.builtinValidation.validateAndMapExtendedBuiltinForStore(
				this,
				value
			) // Will take care of also mapping extensions
		} else {
			validateAndMapExtensionValueForStore(context, value)
		}

	fun validateAndMapExtensionValueForStore(
		context: CustomEntityConfigValidationContext,
		value: RawJson.JsonObject,
	): RawJson.JsonObject {
		val mappedObjectProperties = mutableMapOf<String, RawJson>()
		(properties.keys + value.properties.keys).forEach { propName ->
			val propConfig = properties[propName]
			if (propConfig == null) {
				context.validation.addError("GE-OBJECT-UNKNOWNPROP", "prop" to propName)
			} else {
				val propValue: RawJson? = value.properties[propName]
				val mappedValue = if (propValue == null) {
					if (propConfig.defaultValue == null) {
						context.validation.addError("GE-OBJECT-MISSINGPROP", "prop" to propName)
						null
					} else {
						propConfig.defaultValue.valueForStore()
					}
				} else if (propConfig.defaultValue?.shouldIgnoreForStore(propValue) == true) {
					null
				} else {
					context.validation.appending(".", propName) {
						propConfig.type.validateAndMapValueForStore(context, propValue)
					}
				}
				if (mappedValue != null) {
					mappedObjectProperties[propName] = mappedValue
				}
			}
		}
		return RawJson.JsonObject(mappedObjectProperties)
	}
}