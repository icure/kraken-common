package org.taktik.icure.customentities.config.typing

import kotlinx.serialization.Serializable
import org.taktik.icure.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.customentities.util.CustomEntityValueValidationContext
import org.taktik.icure.entities.RawJson
import org.taktik.icure.jackson.annotations.JsonIgnore
import org.taktik.icure.jackson.annotations.JsonInclude

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
@Serializable
sealed interface GenericTypeConfig {
	val nullable: Boolean

	/**
	 * Verifies that the configuration is valid.
	 * For example, this checks that if the configuration specifies a default value the default value respects
	 * the restrictions imposed by the configuration.
	 * @param context the context of the validation, allowing to resolve references and report errors
	 * @throws IllegalArgumentException if the configuration is not valid
	 */
	fun validateConfig(
		context: CustomEntityConfigValidationContext,
	) {}

	/**
	 * Verifies if a json provided by a user is valid for this configuration, and transforms it if needed before storing
	 * it (to set default values, or update renamed parameters).
	 * @param context the context of the validation, allowing to resolve references and report errors
	 * @param value a value that should be of this type
	 * @return the transformed json value
	 * @throws IllegalArgumentException if the value is not valid
	 */
	fun validateAndMapValueForStore(
		context: CustomEntityValueValidationContext,
		value: RawJson,
	): RawJson

	/**
	 * Dependencies of this type to object definitions (name, builtin).
	 * Checks recursively on collection types but doesn't resolve references to object definitions, so only direct
	 * dependencies are included.
	 */
	@get:JsonIgnore
	val objectDefinitionDependencies: Set<Pair<String, Boolean>>
		get() = emptySet()

	/**
	 * Dependencies of this type to enum definitions (name, builtin).
	 * Checks recursively on collection types but doesn't resolve references to enum definitions, so only direct
	 * dependencies are included.
	 */
	@get:JsonIgnore
	val enumDefinitionDependencies: Set<Pair<String, Boolean>>
		get() = emptySet()

	/**
	 * Check if this type configuration is equal to another one, but ignoring the value of [nullable]
	 */
	fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean

	/**
	 * Check if two raw JSON values are semantically equivalent according to this type configuration.
	 *
	 * If the JSON values are equal always returns true.
	 * If the JSON values are not equal and are not structurally valid for this type, returns false without failing,
	 * the caller should validate separately.
	 *
	 * [resolutionContext] is used by [ObjectTypeConfig] to resolve the object definition and compare
	 * properties accounting for their constant default values. Pass null when no context is available;
	 * the implementation will fall back to structural equality for object types in that case.
	 *
	 * The default implementation is plain structural equality on [RawJson], which is correct for all
	 * primitive type configurations. Collection and object types override this to recurse with
	 * type-aware comparison.
	 */
	fun areEquivalent(a: RawJson, b: RawJson, resolutionContext: CustomEntityConfigResolutionContext? = null): Boolean = a == b
}