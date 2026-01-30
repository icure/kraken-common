package org.taktik.icure.domain.customentities.config.migration

import org.taktik.icure.entities.RawJson

data class ObjectMigration(
	/**
	 * Reference to an object definition in the source configuration
	 */
	val sourceReference: String,
	/**
	 * Reference to an object definition in the target configuration
	 */
	val targetReference: String,
	/**
	 * Specifies how the value for a key in the target object should be obtained.
	 * Takes priority over [fallbackBehavior].
	 */
	val targetMappings: Map<String, ValueProvider> = emptyMap(),
	/**
	 * Specifies how to obtain values for keys in the target object that are not explicitly set in [targetMappings].
	 * On each property of the target object, the behaviors are evaluated in order, and the first that can be applied
	 * is used.
	 *
	 * # Example
	 * - Source(a: Int(0-10), b: Int(0-10), c: Float(0.0-1.0))
	 * - Target(a: Int(0-10), b: Int(0-20) = 10, c: Int(0-100) = 50, d: Boolean = true)
	 * - No [targetMappings]
	 *
	 * With fallbackBehavior = [[FallbackBehavior.ExactMatchFromSourceByName], [FallbackBehavior.UseTargetDefaultIgnoringSource], [FallbackBehavior.CoerceFromSourceByName]]
	 * - a -> from source a (exact match)
	 * - b -> use target default 10 (no exact match, has default; could be coerced but default takes priority)
	 * - c -> use target default 50 (no exact match, has default)
	 * - d -> use target default true
	 *
	 * With fallbackBehavior = [[FallbackBehavior.CoerceFromSourceByName], [FallbackBehavior.UseTargetDefaultIgnoringSource]]
	 * - a -> from source a (exact match is also a coercion)
	 * - b -> from source b coerced to Int[0-20]
	 * - c -> use target default 50 (can't be coerced)
	 * - d -> use target default true
	 *
	 * With fallbackBehavior = [[FallbackBehavior.UseTargetDefaultIgnoringSource], [FallbackBehavior.CoerceFromSourceByName]]
	 * - a -> from source a (no default, can be coerced)
	 * - b -> use target default 10
	 * - c -> use target default 50
	 * - d -> use target default true
	 *
	 * With fallbackBehavior = [[FallbackBehavior.CoerceFromSourceByName]]
	 * - a -> from source a
	 * - b -> from source b
	 * - c -> error: can't be coerced and using the default value is not a fallback option
	 * - d -> error: doesn't exist in source and using the default value is not a fallback option
	 *
	 * With fallbackBehavior = [[FallbackBehavior.CoerceFromSourceByName], [FallbackBehavior.UseTargetDefault]]
	 * - a -> from source a
	 * - b -> from source b
	 * - c -> error: can't be coerced, has a default value but `c` exist also in source so UseTargetDefault can't be
	 *        applied
	 * - d -> use target default true (no source, has default)
	 */
	val fallbackBehavior: List<FallbackBehavior> = emptyList(),
	/**
	 * List of source property names that are explicitly marked as ignored for this migration.
	 * The migration configuration validation will not raise warning for unused source properties that are listed here.
	 * Note that this may include source properties that are actually used in the migration through custom migrations.
	 */
	val explicitlyIgnoredSources: List<String> = emptyList(),
) {
	enum class FallbackBehavior {
		/**
		 * Automatically get the target property value if there is a source property with the same name and have
		 * matching type configurations, including validation rules and nullability.
		 *
		 * An exception is done for object and enum types, which must have the same nullability, but can have different
		 * definitions as long as there is an [ObjectMigration] or [EnumMigration] defined from the source type to the
		 * target type.
		 * If there is no such migration defined for those types then to have a match:
		 * - Two enum definitions must they have exactly the same entries.
		 * - Two object definitions must have exactly the same properties with exactly the same type for each property,
		 *   including nullability and validation rules, except again for object and enum types, which can be different
		 *   as long as there is a corresponding migration defined.
		 *
		 * It is important to note that explicit migrations take priority over this fallback behavior, so if you have
		 * identical object definitions between two versions:
		 * - Obj V1 (foo: Foo)
		 * - Obj V2 (foo: Foo, boo: Int = 42)
		 * - Foo(bar: String)
		 *
		 * And you define object migrations:
		 * - Obj V1 -> Obj V2 with mappings {} fallbackBehavior [ExactMatchFromSourceByName, UseTargetDefault]
		 * - Foo -> Foo with mappings { bar: Custom }
		 *
		 * Then when migrating Obj V1 to Obj V2, the property foo will use the custom migration defined for Foo -> Foo,
		 * even though the definitions of the objects are identical.
		 *
		 * Examples of non-matches:
		 * - A IntTypeConfig "number" with min/max 1-10 on source and min/max 1-20 on target
		 * - A IntTypeConfig "number" with min/max 1-10 on source and a IntTypeConfig "Number" with min/max 1-10 on
		 *   target (different names)
		 * - A IntTypeConfig "number" with min/max 1-10 on source and a FloatTypeConfig "number" with min/max 1.0-10.0
		 *   on target
		 * - A ObjectTypeConfig "address" for definition Address(street: String, zip: Int) on source and a
		 *   ObjectTypeConfig "address" for definition Address(street: String, zip: Int, country: String? = null) on
		 *   target
		 * - A ObjectTypeConfig "address" for definition Address(street: String, zip: Int) on source and a
		 *   ObjectTypeConfig "address" for definition Location(street: String, zip: Int) on target
		 * - A EnumTypeConfig "priority" for enum Priority(LOW, MEDIUM, HIGH) on source and a EnumTypeConfig "priority"
		 *   for enum Priority(LOW, MEDIUM, HIGH, CRITICAL) on target
		 * - A EnumTypeConfig "priority" for enum Priority(LOW, MEDIUM, HIGH) on source and a EnumTypeConfig "priority"
		 *   for enum Severity(LOW, MEDIUM, HIGH) on target
		 * - A StringTypeConfig "name" with maxLength validation 10 on source and a StringTypeConfig "name" with
		 *   maxLength validation 20 on target
		 * - A non-nullable StringTypeConfig "name" on source and a nullable StringTypeConfig "name" on target
		 */
		//TODO for typealias won't resolve: a typealias that resolves to a certain configuration is not an exact match
		// for another typealias that resolves to the same configuration, or for the configuration itself
		ExactMatchFromSourceByName,
		/**
		 * Automatically get the target property value if there is a source property with the same name and a type that
		 * can be coerced to the target type using the rules of [SourceValueTransform.CoerceType].
		 */
		CoerceFromSourceByName,
		/**
		 * If there is no source property with matching name, use the default value of the target property as defined in
		 * the target configuration,
		 */
		UseTargetDefault,
		/**
		 * Use the default value of the target property as defined in the target configuration, independently of the
		 * source.
		 * Compared to [UseTargetDefault] this behavior could hide issues in your migration configuration: if there is
		 * a property that changed type in a way that can't be coerced, you might want to use a custom migration, but
		 * if you configured this fallback behavior and the target property has a default value, you won't get any
		 * error.
		 */
		UseTargetDefaultIgnoringSource,
	}

	sealed interface ValueProvider {
		/**
		 * Use the default value of the target property as defined in the target configuration.
		 * This is allowed only if the target property has a default value and that value is a constant.
		 */
		data object UseTargetDefault : ValueProvider
		/**
		 * Use a predefined constant value as the target property value, independent of the source object, or any
		 * default values for the target property.
		 * The value must be compatible with the target property type configuration.
		 */
		data class Use(
			val value: RawJson
		) : ValueProvider
		/**
		 * Transform the value from a property in the source object to use as the target property value.
		 */
		data class FromSource(
			/**
			 * If null, use the target property name to find the source property.
			 */
			val sourcePropertyName: String? = null,
			/**
			 * Specifies a transformation to apply to the source value before using it as the target value.
			 * If null the source value must match exactly, as explained in [FallbackBehavior.ExactMatchFromSourceByName],
			 * otherwise applicability rules depend on the specific [SourceValueTransform] used.
			 */
			val transform: SourceValueTransform? = null
		) : ValueProvider

		/**
		 * Use a custom migration function to obtain the target property value.
		 * For each target property with this configuration, a separate custom migration function must be provided.
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
		data object Custom : ValueProvider
	}

	sealed interface SourceValueTransform {
		/**
		 * Automatically convert the source type to the target type.
		 * This configuration can only be used if all possible values of the source type are valid values for the target
		 * type, or can be converted without losing any information.
		 *
		 * The supported conversions are:
		 * - Nullable and non-nullable source types can be converted to nullable target types, as long as the rest of
		 *   the type configuration can be coerced. Nullable source types cannot be coerced to non-nullable target
		 *   types.
		 * - Identical source and target type configurations, including validation rules.
		 * - Any type configuration to a JsonTypeConfig
		 * - IntTypeConfig to IntTypeConfig, IntTypeConfig to FloatTypeConfig, or FloatTypeConfig to FloatTypeConfig
		 *   if the range of the target type covers the entire range of the source type.
		 * - StringTypeConfig to StringTypeConfig if the validation rules of the target type are less restrictive, i.e.
		 *   the maxLength of the target is greater than or equal to the maxLength of the source, and the minLength of
		 *   the target is less than or equal to the minLength of the source.
		 * - EnumTypeConfig to EnumTypeConfig if an explicit [EnumMigration] is configured (prioritized), or all entries
		 *   of the source enum exist in the target enum
		 * - ObjectTypeConfig to ObjectTypeConfig if an explicit [ObjectMigration] is configured (prioritized), or all
		 *   properties of the target value can be mapped from the source value using the [fallbackBehavior] configured
		 *   in this [ObjectMigration].
		 * - TODO more?
		 *
		 * Examples of unsupported conversions:
		 * - A FloatTypeConfig with min/max 1.0-10.0 to a IntTypeConfig "number" with min/max 1-10 (not all source
		 *   values can be represented exactly in target)
		 * - A StringTypeConfig with no minLength validation (implicitly 0) to a StringTypeConfig with minLength
		 *   validation 3 (The source value "a", it is not valid for the target, )
		 *
		 * Note that even when coercion is possible, it is not always the correct choice, for example:
		 * - A FloatTypeConfig "percent" with min/max 0.0-1.0 can be coerced to a FloatTypeConfig "percent" with min/max
		 *   0.0-100.0, but you might want to apply a scaling instead (multiply by 100).
		 *
		 * # Nested object coercion
		 *
		 * ## Example 1
		 *
		 * - ObjV1(foo: FooV1)
		 * - FooV1(bar: String)
		 * - ObjV2(foo: FooV2, boo: Int = 42)
		 * - FooV2(bar: String, baz: Boolean = true)
		 *
		 * ### Invalid migration 1
		 * - ObjV1 -> ObjV2 with mappings { boo: UseTargetDefault }, fallbackBehavior [ CoerceFromSourceByName ]
		 *
		 * This migration is invalid because FooV1 cannot be coerced to FooV2.
		 * The implicit rule for migration FooV1 -> FooV2 is with empty mappings and the same fallbackBehavior as
		 * ObjV1 -> ObjV2.
		 * Since Foo.baz does not exist in FooV1 and the fallback behavior doesn't allow for default we can't coerce.
		 *
		 * ### Invalid migration 2
		 * - ObjV1 -> ObjV2 with mappings {}, fallbackBehavior [ UseTargetDefault ]
		 *
		 * This migration is invalid because coercion is not allowed as a fallback behavior so we cannot go from
		 * FooV1 to FooV2.
		 *
		 * ### Valid migration 1
		 * - ObjV1 -> ObjV2 with mappings {}, fallbackBehavior [ CoerceFromSourceByName, UseTargetDefault ]
		 *
		 * This migration is valid: boo uses the default from the fallback behavior, and foo can be coerced from FooV1
		 * to FooV2 by implicitly considering the migration rule for FooV1 -> FooV2 with empty mappings {} and same
		 * fallbackBehavior as the ObjV1 -> ObjV2 configuration.
		 *
		 * ### Valid migration 2 (equivalent)
		 * - ObjV1 -> ObjV2 with mappings { foo: FromSource(transform: CoerceValue) }, fallbackBehavior [ UseTargetDefault ]
		 */
		data object CoerceType : SourceValueTransform
		// T -> [T]
//		data object WrapToSingletonList : SourceValueTransform
		// T -> { key: T }
//		data class WrapToSingletonMap(
//			val key: String
//		) : SourceValueTransform
		// Number -> Number with incompatible range
//		data object CoerceToRange : SourceValueTransform
		// Number -> Number with different range
//		data object ScaleToRange : SourceValueTransform // Rounding option?
		// Double -> Int
//		data object Round : SourceValueTransform // Rounding option
		// String -> String, List -> List
//		data object Truncate : SourceValueTransform // Truncate start/end/middle?
	}
}

