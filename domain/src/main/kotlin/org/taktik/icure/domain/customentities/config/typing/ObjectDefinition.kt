package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.taktik.icure.domain.customentities.config.ExtendableEntityName
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.addError
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
	val baseEntity: ExtendableEntityName? = null
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
				resolutionContext: CustomEntityConfigResolutionContext,
				context: ScopedErrorCollector
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
					resolutionContext: CustomEntityConfigResolutionContext,
					context: ScopedErrorCollector,
				) {
					// Technically supported on all types even though doesn't really make sense to have a "constant" id or date
					// For simplicity will probably just hide it on the frontend
					typeConfig.validateAndMapValueForStore(resolutionContext, context, value)
				}

				override fun valueForStore(): RawJson? = null

				override fun shouldIgnoreForStore(value: RawJson): Boolean =
					value == this.value

				override val isConstant: Boolean = true
			}

			data object GenerateUuidV4 : DefaultValue {
				override suspend fun validateFor(
					typeConfig: GenericTypeConfig,
					resolutionContext: CustomEntityConfigResolutionContext,
					context: ScopedErrorCollector,
				) {
					if (
						typeConfig !is UuidTypeConfig
					) {
						context.addError("GE-OBJECT-DEFAULT-UUIDV4TYPE")
					}
				}

				override fun valueForStore(): RawJson =
					RawJson.JsonString(UUID.randomUUID().toString())

				override fun shouldIgnoreForStore(value: RawJson): Boolean =
					false

				override val isConstant: Boolean = false
			}

			@JsonInclude(JsonInclude.Include.NON_DEFAULT)
			class NowDateTime(
				val zoneId: String? = null
			) : DefaultValue {
				override suspend fun validateFor(
					typeConfig: GenericTypeConfig,
					resolutionContext: CustomEntityConfigResolutionContext,
					context: ScopedErrorCollector,
				) {
					if (
						typeConfig !is FuzzyDateTimeTypeConfig
					) {
						context.addError("GE-OBJECT-DEFAULT-NOWDATETIMETYPE")
					}
					zoneId?.let {
						if (!Validation.validZoneId(it)) {
							context.addError("GE-ZONEID",  "value" to it)
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

				override val isConstant: Boolean = false
			}

			@JsonInclude(JsonInclude.Include.NON_DEFAULT)
			class NowDate(
				val zoneId: String? = null
			) : DefaultValue {
				override suspend fun validateFor(
					typeConfig: GenericTypeConfig,
					resolutionContext: CustomEntityConfigResolutionContext,
					context: ScopedErrorCollector,
				) {
					if (
						typeConfig !is FuzzyDateTypeConfig
					) {
						context.addError("GE-OBJECT-DEFAULT-NOWDATETYPE")
					}
					zoneId?.let {
						if (!Validation.validZoneId(it)) {
							context.addError("GE-ZONEID", "value" to it)
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
					resolutionContext: CustomEntityConfigResolutionContext,
					context: ScopedErrorCollector,
				) {
					if (
						typeConfig !is FuzzyTimeTypeConfig
					) {
						context.addError("GE-OBJECT-DEFAULT-NOWTIMETYPE")
					}
					zoneId?.let {
						if (!Validation.validZoneId(it)) {
							context.addError("GE-ZONEID", "value" to it)
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
		resolutionContext: CustomEntityConfigResolutionContext,
		context: ScopedErrorCollector
	) {
		if (properties.isEmpty() && baseEntity == null) {
			context.addWarning("GE-OBJECT-WEMPTY")
		}
		context.appending(".") {
			properties.forEach { (propName, propConfig) ->
				context.appending(propName) {
					validateIdentifier(context, propName)
					propConfig.type.validateConfig(resolutionContext, context)
					context.appending("<DEFAULT>") {
						propConfig.defaultValue?.validateFor(propConfig.type, resolutionContext, context)
					}
				}
			}
		}
		if (baseEntity != null) {
			TODO("Validate base entity and check for conflict on props (give warning)")
		}
	}

	fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		context: ScopedErrorCollector,
		value: RawJson.JsonObject,
	): RawJson.JsonObject {
		if (baseEntity != null) {
			TODO("Handle base entity")
		}
		val mappedObjectProperties = mutableMapOf<String, RawJson>()
		(properties.keys + value.properties.keys).forEach { propName ->
			val propConfig = properties[propName]
			if (propConfig == null) {
				context.addError("GE-OBJECT-UNKNOWNPROP", "prop" to propName)
			} else {
				val propValue: RawJson? = value.properties[propName]
				val mappedValue = if (propValue == null) {
					if (propConfig.defaultValue == null) {
						context.addError("GE-OBJECT-MISSINGPROP", "prop" to propName)
						null
					} else {
						propConfig.defaultValue.valueForStore()
					}
				} else if (propConfig.defaultValue?.shouldIgnoreForStore(propValue) == true) {
					null
				} else {
					context.appending(".", propName) {
						propConfig.type.validateAndMapValueForStore(resolutionContext, context, propValue)
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