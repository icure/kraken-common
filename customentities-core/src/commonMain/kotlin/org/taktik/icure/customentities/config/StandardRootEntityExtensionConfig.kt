package org.taktik.icure.customentities.config

import org.taktik.icure.jackson.annotations.JsonInclude
import org.taktik.icure.jackson.annotations.JsonIncludeValue
import org.taktik.icure.customentities.config.typing.ObjectDefinition

@JsonInclude(JsonIncludeValue.NON_DEFAULT)
data class StandardRootEntityExtensionConfig(
	/**
	 * Reference to the object definition (i.e. key of [VersionedCustomEntitiesConfiguration.objects]) for this entity
	 * extensions.
	 * This value is also used as the name for the type in the generated custom SDK.
	 * The referenced object definition must have a [ObjectDefinition.baseEntity] that is equal to the root builtin
	 * entity being extended.
	 */
	val objectDefinitionReference: String,
	/**
	 * Version of the configuration for this specific entity.
	 * If an update to a [VersionedCustomEntitiesConfiguration] changes nothing in the definitions of this entity
	 * extension, and on types this extension depends on, this version must be kept the same.
	 */
	val version: Int,
)

@JsonInclude(JsonIncludeValue.NON_DEFAULT)
data class StandardRootEntitiesExtensionConfig(
	override val patient: StandardRootEntityExtensionConfig? = null,
	override val healthcareParty: StandardRootEntityExtensionConfig? = null,
) : ExtendableRootEntitiesConfiguration<StandardRootEntityExtensionConfig>