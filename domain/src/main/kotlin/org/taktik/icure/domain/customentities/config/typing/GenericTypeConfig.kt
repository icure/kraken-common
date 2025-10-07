package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.databind.JsonNode
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath

/**
 * Common supertype for strong validation configurations of a custom entity content or custom extensions on an entity.
 *
 * # Default
 *
 * Most types allow to specify a default value, which is constant (see [ConstantDefaultValue]) or generated as needed.
 *
 * This value is used when an entry for (see [ObjectDefinition])
 *
 * # Nullability
 *
 * Most type configurations allow to specify if null values are accepted.
 *
 * Note: even if a configuration allows for null values, unless a default
 */
sealed interface GenericTypeConfig {
	/**
	 * Verifies that the configuration is valid.
	 * For example, this checks that if the configuration specifies a default value the default value respects
	 * the restrictions imposed by the configuration.
	 * @param resolutionContext allows retrieving custom types definitions needed to validate the config
	 * @param path path of the configuration, allows to give better error reporting
	 * @throws IllegalArgumentException if the configuration is not valid
	 */
	fun validateConfig(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
	) {}

	/**
	 * Verifies if a json provided by a user is valid for this configuration, and transforms it if needed before storing
	 * it (to set default values, or update renamed parameters).
	 * @param resolutionContext allows retrieving custom types definitions needed to validate the value
	 * @param path path of the configuration, allows to give better error reporting
	 * @param value a value that should be of this type
	 * @return the transformed json value
	 * @throws IllegalArgumentException if the value is not valid
	 */
	fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: JsonNode,
	): JsonNode

	/**
	 * Transforms a json retrieved from the database before returning it to the user (to set implicit default values, or
	 * update renamed parameters).
	 * There is no validation done by this method; invalid types or values are ignored.
	 * @param resolutionContext allows retrieving custom types definitions needed to validate the value
	 * @param value a value that should be of this type, or null if there is no value set.
	 * @return the transformed json value
	 */
	fun mapValueForRead(
		resolutionContext: CustomEntityConfigResolutionContext,
		value: JsonNode,
	): JsonNode = value

	val shouldMapForRead : Boolean get() = false
}