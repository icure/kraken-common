package org.taktik.icure.domain.customentities.config.migration

import org.taktik.icure.entities.RawJson

/**
 * Specifies how to get the value of a property in a target object.
 * These configurations can be applied in any of the following cases:
 * - When migrating from a different version of the same object definition (the source object)
 * - When migrating from an entirely different object definition (the source object)
 * - When adding a new extension to a standard entity that was not using any extension before (no source object)
 */
sealed interface PropertyValueProvider : MigratingPropertyValueProvider {
	/**
	 * Use the default value of the target property as defined in the target object configuration.
	 * This is allowed only if the target property has a default value and that value is a constant.
	 */
	data object UseTargetDefault : PropertyValueProvider

	/**
	 * Use a predefined constant value as the target property value, independently of the source object or default
	 * values for the target property, if any.
	 * The value must be compatible with the target property type configuration.
	 */
	data class Use(
		val value: RawJson
	) : PropertyValueProvider

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
	data object Custom : PropertyValueProvider
}

/**
 * Specifies how to get the value of a property in a target object given a source object:
 * These configurations can be applied in any of the following cases:
 * - When migrating from a different version of the same object definition (the source object)
 * - When migrating from an entirely different object definition (the source object)
 * Unlike [PropertyValueProvider], these configurations cannot be used when there is no source object.
 */
sealed interface MigratingPropertyValueProvider {
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
		 * If null the source value must match exactly, as explained in [ObjectMigration.FallbackBehavior.ExactMatchFromSourceByName],
		 * otherwise applicability rules depend on the specific [ValueTransformer] used.
		 */
		val transform: ValueTransformer? = null
	) : PropertyValueProvider
}
