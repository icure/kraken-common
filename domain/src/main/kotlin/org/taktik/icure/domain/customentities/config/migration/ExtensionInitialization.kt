package org.taktik.icure.domain.customentities.config.migration

/**
 * Specifies how to initialize a newly added extension in a standard entity that didn't have it before.
 */
data class ExtensionInitialization(
	/**
	 * Specifies how the value for each property in the target extension should be obtained.
	 * The key is the property name in the target extension.
	 */
	val propertyConfigurations: Map<String, PropertyValueProvider> = emptyMap(),
	/**
	 * If true, for properties not explicitly configured in [propertyConfigurations], the default value defined in the
	 * target extension configuration will be used.
	 * If false, an error will be raised if a property is missing from [propertyConfigurations].
	 */
	val implicitlyUseTargetDefault: Boolean = false,
)