package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath
import org.taktik.icure.utils.FuzzyDates
import org.taktik.icure.utils.Validation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class ObjectDefinition(
	val properties: Map<String, PropertyConfiguration>
) {
	/*TODO
	 * - Ignore fields, list of fields that should be ignored (to support deletion)
	 * - Field name aliases
	 */
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
				resolutionPath: ResolutionPath
			)

			/**
			 * Get a value matching this default configuration; if not null the value must be stored in the DB.
			 * May change at each invocation
			 */
			fun valueForStore(): RawJson?

			/**
			 * Get a value matching this default configuration; not null only for configurations where the value
			 * should not be stored.
			 */
			fun valueForRead(): RawJson?


			/**
			 * If the provided value should be ignored according to this default configuration.
			 */
			fun shouldIgnoreForStore(value: RawJson): Boolean

			/**
			 * Represents a constant default value.
			 */
			@JsonInclude(JsonInclude.Include.NON_DEFAULT)
			data class Constant(
				/**
				 * If false, if a field's value matches the configured default, it is not saved in the database.
				 * If true, a field's value is always stored, even if it matches the configured default.
				 */
				val storeExplicitly: Boolean = false,
				/**
				 * The default value
				 */
				@param:JsonInclude(JsonInclude.Include.ALWAYS)
				val value: RawJson,
			) : DefaultValue {
				override suspend fun validateFor(
					typeConfig: GenericTypeConfig,
					resolutionContext: CustomEntityConfigResolutionContext,
					resolutionPath: ResolutionPath
				) {
					// Technically supported on all types even though doesn't really make sense to have a "constant" id or date
					// For simplicity will probably just hide it on the frontend
					typeConfig.validateAndMapValueForStore(resolutionContext, resolutionPath, value)
				}

				override fun valueForStore(): RawJson? = if (storeExplicitly) value else null

				override fun valueForRead(): RawJson? = if (storeExplicitly) null else value

				override fun shouldIgnoreForStore(value: RawJson): Boolean =
					!storeExplicitly && this.value == value
			}

			data object GenerateUuidV4 : DefaultValue {
				override suspend fun validateFor(
					typeConfig: GenericTypeConfig,
					resolutionContext: CustomEntityConfigResolutionContext,
					resolutionPath: ResolutionPath
				) {
					require(
						typeConfig is UuidTypeConfig
					) {
						"$resolutionPath: GenerateUuidV4 default value can only be applied to UUID type."
					}
				}

				override fun valueForStore(): RawJson =
					RawJson.JsonString(UUID.randomUUID().toString())

				override fun valueForRead(): Nothing? = null

				override fun shouldIgnoreForStore(value: RawJson): Boolean =
					false
			}

			@JsonInclude(JsonInclude.Include.NON_DEFAULT)
			class NowDateTime(
				val zoneId: String? = null
			) : DefaultValue {
				override suspend fun validateFor(
					typeConfig: GenericTypeConfig,
					resolutionContext: CustomEntityConfigResolutionContext,
					resolutionPath: ResolutionPath
				) {
					require(
						typeConfig is FuzzyDateTimeTypeConfig
					) {
						"$resolutionPath: NowDateTime default value can only be applied to fuzzy date time type."
					}
					zoneId?.let {
						require(Validation.validZoneId(it)) {
							"$resolutionPath: invalid zone id"
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

				override fun valueForRead(): Nothing? = null

				override fun shouldIgnoreForStore(value: RawJson): Boolean =
					false
			}

			@JsonInclude(JsonInclude.Include.NON_DEFAULT)
			class NowDate(
				val zoneId: String? = null
			) : DefaultValue {
				override suspend fun validateFor(
					typeConfig: GenericTypeConfig,
					resolutionContext: CustomEntityConfigResolutionContext,
					resolutionPath: ResolutionPath
				) {
					require(
						typeConfig is FuzzyDateTypeConfig
					) {
						"$resolutionPath: NowDate default value can only be applied to fuzzy date type."
					}
					zoneId?.let {
						require(Validation.validZoneId(it)) {
							"$resolutionPath: invalid zone id"
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

				override fun valueForRead(): Nothing? = null

				override fun shouldIgnoreForStore(value: RawJson): Boolean =
					false
			}

			@JsonInclude(JsonInclude.Include.NON_DEFAULT)
			class NowTime(
				val zoneId: String? = null
			) : DefaultValue {
				override suspend fun validateFor(
					typeConfig: GenericTypeConfig,
					resolutionContext: CustomEntityConfigResolutionContext,
					resolutionPath: ResolutionPath
				) {
					require(
						typeConfig is FuzzyTimeTypeConfig
					) {
						"$resolutionPath: NowTime default value can only be applied to fuzzy time type."
					}
					zoneId?.let {
						require(Validation.validZoneId(it)) {
							"$resolutionPath: invalid zone id"
						}
					}
				}

				override fun valueForStore(): RawJson =
					RawJson.JsonInteger(
						FuzzyDates.getFuzzyTime(
							LocalTime.now(zoneId?.let { ZoneId.of(it) } ?: ZoneOffset.UTC),
						).toLong()
					)

				override fun valueForRead(): Nothing? = null

				override fun shouldIgnoreForStore(value: RawJson): Boolean =
					false
			}
		}
	}

	suspend fun validateDefinition(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath
	) {
		properties.forEach { (propName, propConfig) ->
			path.appending(".", propName) {
				propConfig.type.validateConfig(resolutionContext, path)
				path.appending("<DEFAULT>.") {
					propConfig.defaultValue?.validateFor(propConfig.type, resolutionContext, path)
				}
			}
		}
	}

	fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: RawJson.JsonObject,
	): RawJson.JsonObject {
		val mappedObjectProperties = mutableMapOf<String, RawJson>()
		(properties.keys + value.properties.keys).forEach { propName ->
			val propConfig = properties[propName]
			requireNotNull(propConfig) {
				"$path: unexpected property $propName"
			}
			val propValue: RawJson? = value.properties[propName]
			val mappedValue = if (propValue == null) {
				require (propConfig.defaultValue != null) {
					"$path: missing required property $propName (no default)"
				}
				propConfig.defaultValue.valueForStore()
			} else if (propConfig.defaultValue?.shouldIgnoreForStore(propValue) == true) {
				null
			} else {
				path.appending(".", propName) {
					propConfig.type.validateAndMapValueForStore(resolutionContext, path, propValue)
				}
			}
			if (mappedValue != null) {
				mappedObjectProperties[propName] = mappedValue
			}
		}
		return RawJson.JsonObject(mappedObjectProperties)
	}

	fun mapValueForRead(
		resolutionContext: CustomEntityConfigResolutionContext,
		value: RawJson.JsonObject,
	): RawJson.JsonObject = RawJson.JsonObject(
		properties.entries.mapNotNull { (propId, propConfig) ->
			(
				// A property with explicit null valur is actually going to be a NullNode, not an actual `null` -> will be mapped accordingly
				value.properties[propId]?.let {
					if (propConfig.type.shouldMapForRead) {
						propConfig.type.mapValueForRead(resolutionContext, it)
					} else {
						it
					}
				} ?: propConfig.defaultValue?.valueForRead()
			)?.let {
				Pair(propId, it)
			}
		}.toMap()
	)
}