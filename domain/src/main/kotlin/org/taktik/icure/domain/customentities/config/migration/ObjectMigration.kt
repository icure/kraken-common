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
	val targetMappings: Map<String, PropertyValueProvider> = emptyMap(),
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
	// Changed plan: going to always emit a warning and the cockpit that can decide to show it as an error
//	/**
//	 * List of source property names that are explicitly marked as ignored for this migration.
//	 * If a property of the source object is not used by a builtin migration configuration and is not listed here, then
//	 * the configuration validation will raise an error to point at potential mistakes which could result in data loss.
//	 * If a source property has been intentionally deleted or is used in a custom migration function, then it must be
//	 * included here.
//	 */
//	val explicitlyIgnoredSources: List<String> = emptyList(),
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
		 * - Foo V1==V2 (bar: String)
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
		 * can be coerced to the target type using the rules of [ValueTransformer.CoerceType].
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

}
