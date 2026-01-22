package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath

//TODO
// - add BuiltInTypeConfig to allow referencing built-in embedded complex types (like Address, CodeStub, ...)?
//   - Could be convenient
//   - Not necessary (people can make the custom type themselves)
//   - Must also allow extensions on them, defined at the level of the VersionedCustomEntitiesConfiguration
//   - Could be adding complexity to the getting into cardinal, the custom entities configuration could become too overwhelming
//   - have to think how to efficiently handle validation and DTO<->Stored entity mapping
// - add TypeAliasTypeConfig to allow reusing type configurations with specific validation, and have them mapped on the SDK side with a type specialization
// - Allow custom validators that only apply on client side?
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
@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	property = "type",
)
@JsonSubTypes(
	JsonSubTypes.Type(value = BooleanTypeConfig::class, name = "Boolean"),
	JsonSubTypes.Type(value = EnumTypeConfig::class, name = "Enum"),
	JsonSubTypes.Type(value = FloatTypeConfig::class, name = "Float"),
	JsonSubTypes.Type(value = FuzzyDateTimeTypeConfig::class, name = "FuzzyDateTime"),
	JsonSubTypes.Type(value = FuzzyDateTypeConfig::class, name = "FuzzyDate"),
	JsonSubTypes.Type(value = FuzzyTimeTypeConfig::class, name = "FuzzyTime"),
	JsonSubTypes.Type(value = IntTypeConfig::class, name = "Int"),
	JsonSubTypes.Type(value = JsonTypeConfig::class, name = "Json"),
	JsonSubTypes.Type(value = ListTypeConfig::class, name = "List"),
	JsonSubTypes.Type(value = MapTypeConfig::class, name = "Map"),
	JsonSubTypes.Type(value = ObjectTypeConfig::class, name = "Object"),
	JsonSubTypes.Type(value = StringTypeConfig::class, name = "String"),
	JsonSubTypes.Type(value = UuidTypeConfig::class, name = "Uuid"),
)
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
		value: RawJson,
	): RawJson

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
		value: RawJson,
	): RawJson = value

	val shouldMapForRead : Boolean @JsonIgnore get() = false
}