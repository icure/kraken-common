package org.taktik.icure.domain.customentities.config.migration

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Specifies how to convert values from a source enum to a target enum in different configurations.
 * A valid configuration must specify how to map all possible values of the source enum to values of the target enum,
 * implicitly through [automaticallyMapIdenticalNames], or explicitly through [sourceMappings] or [fallbackMapping].
 * A valid configuration can use all values of the target enum, but it is not a requirement.
 *
 * ## Examples
 *
 * Assume we have the following enums:
 * - **Source enum** `Priority`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
 * - **Target enum** `Severity`: `LOW`, `MEDIUM`, `HIGH`, `URGENT`
 *
 * ### Example 1: Partial explicit mapping with automatic identical names
 * ```kotlin
 * EnumMigration(
 *     sourceReference = "Priority",
 *     targetReference = "Severity",
 *     mapping = mapOf("CRITICAL" to TargetValue.Use("URGENT")),
 *     automaticallyMapIdenticalNames = true
 * )
 * ```
 * Result:
 * - `LOW` → `LOW` (automatic)
 * - `MEDIUM` → `MEDIUM` (automatic)
 * - `HIGH` → `HIGH` (automatic)
 * - `CRITICAL` → `URGENT` (explicit mapping)
 *
 * ### Example 2: Fallback mapping
 * ```kotlin
 * EnumMigration(
 *     sourceReference = "Priority",
 *     targetReference = "Severity",
 *     automaticallyMapIdenticalNames = true,
 *     fallbackMapping = TargetValue.Use("URGENT")
 * )
 * ```
 * Result:
 * - `LOW` → `LOW` (automatic)
 * - `MEDIUM` → `MEDIUM` (automatic)
 * - `HIGH` → `HIGH` (automatic)
 * - `CRITICAL` → `URGENT` (fallback, no matching name in target)
 *
 * ### Example 3: Explicit mapping overriding with no automatic and fallback
 * ```kotlin
 * EnumMigration(
 *     sourceReference = "Priority",
 *     targetReference = "Severity",
 *     mapping = mapOf(
 *         "LOW" to TargetValue.Use("MEDIUM"),
 *         "MEDIUM" to TargetValue.Use("HIGH"),
 *     ),
 *     automaticallyMapIdenticalNames = false,
 *     fallbackMapping = TargetValue.Use("URGENT")
 * )
 * ```
 * Result:
 * - `LOW` → `MEDIUM` (explicit mapping)
 * - `MEDIUM` → `HIGH` (explicit mapping)
 * - `HIGH` → `URGENT` (fallback)
 * - `CRITICAL` → `URGENT` (fallback)
 *
 * ### Example 4: Explicit mapping overriding with automatic and fallback
 * ```kotlin
 * EnumMigration(
 *     sourceReference = "Priority",
 *     targetReference = "Severity",
 *     mapping = mapOf(
 *         "LOW" to TargetValue.Use("MEDIUM"),
 *         "MEDIUM" to TargetValue.Use("HIGH"),
 *     ),
 *     automaticallyMapIdenticalNames = true,
 *     fallbackMapping = TargetValue.Use("URGENT")
 * )
 * ```
 * Result:
 * - `LOW` → `MEDIUM` (explicit mapping)
 * - `MEDIUM` → `HIGH` (explicit mapping)
 * - `HIGH` → `HIGH` (automatic)
 * - `CRITICAL` → `URGENT` (fallback)
 *
 * ### Example 5: No explicit mapping, only automatic and fallback
 * ```kotlin
 * EnumMigration(
 *     sourceReference = "Priority",
 *     targetReference = "Severity",
 *     automaticallyMapIdenticalNames = true,
 *     fallbackMapping = TargetValue.Custom
 * )
 * ```
 * Result:
 * - `LOW` → `LOW` (automatic)
 * - `MEDIUM` → `MEDIUM` (automatic)
 * - `HIGH` → `HIGH` (automatic)
 * - `CRITICAL` → custom logic implementation required
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class EnumMigration(
	/**
	 * Reference to an enum definition in the source configuration, or a builtin enum class name if [builtinSource] is true.
	 */
	val sourceReference: String,
	/**
	 * Set to true if [sourceReference] is a reference to a builtin enum.
	 */
	val builtinSource: Boolean = false,
	/**
	 * Reference to an enum definition in the target configuration, or a builtin enum class name if [builtinTarget] is true.
	 */
	val targetReference: String,
	/**
	 * Set to true if [targetReference] is a reference to a builtin enum.
	 */
	val builtinTarget: Boolean = false,
	/**
	 * Specify how to map a certain value of the source enum to a value of the target enum.
	 * Each key must be a valid entry name in the source enum.
	 * Takes priority over [automaticallyMapIdenticalNames] and [fallbackMapping].
	 */
	val sourceMappings: Map<String, TargetValue> = emptyMap(),
	/**
	 * If not explicitly set to false (default), all values of the source enum that have the same name as an entry in
	 * the target enum, are automatically mapped to the corresponding entry.
	 * Takes priority over [fallbackMapping].
	 */
	val automaticallyMapIdenticalNames: Boolean = false,
	/**
	 * Specifies how to map source enum values that:
	 * - Do not have an entry in [sourceMappings], and
	 * - Are not automatically mapped through [automaticallyMapIdenticalNames]. This is the case if:
	 *   - [automaticallyMapIdenticalNames] is set to false, or
	 *   - The source enum value does not have a corresponding entry in the target enum.
	 * When migrating from a custom enum this value is required only if not all mappings are specified through
	 * [sourceMappings] or [automaticallyMapIdenticalNames], and is ignored otherwise.
	 * When migrating from a builtin enum this value is required always required, since, though unlikely, a builtin enum
	 * might get additional fields independently of the custom entity version.
	 */
	val fallbackMapping: TargetValue? = null,
) {
	/**
	 * Specifies how to get a value for the target enum.
	 */
	@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		property = "type",
	)
	@JsonSubTypes(
		JsonSubTypes.Type(value = TargetValue.Use::class, name = "Use"),
		JsonSubTypes.Type(value = TargetValue.Null::class, name = "Null"),
		JsonSubTypes.Type(value = TargetValue.Custom::class, name = "Custom"),
	)
	sealed interface TargetValue {
		/**
		 * Use [value] of the target enum.
		 * [value] must be a valid entry name in the target enum.
		 */
		data class Use(val value: String) : TargetValue
		/**
		 * Map value to null.
		 * This value is valid only if in all cases where this migration would be used the target allows for nullable
		 * values (according [org.taktik.icure.domain.customentities.config.typing.EnumTypeConfig.nullable]).
		 */
		data object Null : TargetValue
		/**
		 * The mapping should use custom logic, to be implemented on the custom SDK side.
		 * The SDK generator will create an interface with a method to implement this custom logic.
		 * Only one method will be created per EnumMigration instance, even if multiple source values use [Custom]
		 * mapping: the source value will be passed as a parameter to the method.
		 *
		 * The return type of the method will be the target enum type with nullability depending on how this migration
		 * is used: if this migration is used only in situations where the target is nullable then the return type of
		 * the custom migration method will also be nullable; if there is at least one situation where this migration is
		 * used where the target is not nullable, then the return type of the custom migration method will not be
		 * nullable.
		 *
		 * Note that views that depend on values obtained through custom migration functions will ignore any entity
		 * that are not yet migrated, so you should rely on builtin migration solutions when possible.
		 *
		 * It is recommended that custom migration function implementations depend solely on the provided input value.
		 * Migration functions that depend on other sources that can vary across different instances of the application,
		 * or between different invocations (random values, time, a mutable state, device information, etc.) may
		 * cause issues.
		 * This could cause inconsistencies, for example, if you have two different processes that are migrating the
		 * same object, or if an object is not saved after migration, and must be migrated again a second time.
		 */
		// TODO don't think there is a use case for that, but if ever needed consider adding a boolean flag that says to divide the custom implementation in a custom for non-nullable target and a custom for nullable target.
		data object Custom : TargetValue
	}
}