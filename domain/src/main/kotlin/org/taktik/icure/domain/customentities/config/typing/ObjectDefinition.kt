package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
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

data class ObjectDefinition(
	val properties: Map<String, PropertyConfiguration>
) {
	/*TODO
	 * - Ignore fields, list of fields that should be ignored (to support deletion)
	 * - Field name aliases
	 */

	data class PropertyConfiguration(
		val type: GenericTypeConfig,
		val defaultValue: DefaultValue? = null
	) {
		/**
		 * If a property defines a default value and is not explicitly provided in the object it is implied that it
		 * should use the default value
		 */
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
			fun valueForStore(): JsonNode?

			/**
			 * Get a value matching this default configuration; not null only for configurations where the value
			 * should not be stored.
			 */
			fun valueForRead(): JsonNode?


			/**
			 * If the provided value should be ignored according to this default configuration.
			 */
			fun shouldIgnoreForStore(value: JsonNode): Boolean

			/**
			 * Represents a constant default value.
			 */
			data class Constant(
				/**
				 * If false, if a field's value matches the configured default, it is not saved in the database.
				 * If true, a field's value is always stored, even if it matches the configured default.
				 */
				val storeExplicitly: Boolean,
				/**
				 * The default value
				 */
				val value: JsonNode,
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

				override fun valueForStore(): JsonNode? = if (storeExplicitly) value else null

				override fun valueForRead(): JsonNode? = if (storeExplicitly) null else value

				override fun shouldIgnoreForStore(value: JsonNode): Boolean =
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

				override fun valueForStore(): JsonNode =
					TextNode(UUID.randomUUID().toString())

				override fun valueForRead(): Nothing? = null

					override fun shouldIgnoreForStore(value: JsonNode): Boolean =
					false
			}

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

				override fun valueForStore(): JsonNode =
					LongNode(
						FuzzyDates.getFuzzyDateTime(
							LocalDateTime.now(zoneId?.let { ZoneId.of(it) } ?: ZoneOffset.UTC),
							ChronoUnit.SECONDS,
							false
						)
					)

				override fun valueForRead(): Nothing? = null

				override fun shouldIgnoreForStore(value: JsonNode): Boolean =
					false
			}

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

				override fun valueForStore(): JsonNode =
					IntNode(
						FuzzyDates.getFuzzyDate(
							LocalDate.now(zoneId?.let { ZoneId.of(it) } ?: ZoneOffset.UTC),
							ChronoUnit.DAYS,
							false
						)
					)

				override fun valueForRead(): Nothing? = null

				override fun shouldIgnoreForStore(value: JsonNode): Boolean =
					false
			}

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

				override fun valueForStore(): JsonNode =
					IntNode(
						FuzzyDates.getFuzzyTime(
							LocalTime.now(zoneId?.let { ZoneId.of(it) } ?: ZoneOffset.UTC),
						)
					)

				override fun valueForRead(): Nothing? = null

				override fun shouldIgnoreForStore(value: JsonNode): Boolean =
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
		value: ObjectNode,
	): ObjectNode {
		val mappedObject: ObjectNode = JsonNodeFactory.instance.objectNode()
		LinkedHashSet<String>(properties.size + value.size()).apply {
			addAll(properties.keys)
			value.fieldNames().forEach { add(it) }
		}.forEach { propName ->
			val propConfig = properties[propName]
			requireNotNull(propConfig) {
				"$path: unexpected property $propName"
			}
			val propValue: JsonNode? = value[propName]
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
				mappedObject.replace(propName, mappedValue)
			}
		}
		return mappedObject
	}

	fun mapValueForRead(
		resolutionContext: CustomEntityConfigResolutionContext,
		value: ObjectNode,
	): ObjectNode = ObjectNode(
		JsonNodeFactory.instance,
		properties.entries.mapNotNull { (propId, propConfig) ->
			(
				value[propId]?.let {
					if (propConfig.type.shouldMapForRead) {
						propConfig.type.mapValueForRead(resolutionContext, value)
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