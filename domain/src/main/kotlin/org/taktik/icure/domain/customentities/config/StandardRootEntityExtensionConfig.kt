package org.taktik.icure.domain.customentities.config

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.domain.customentities.config.typing.ObjectDefinition

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
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
	/**
	 * A map to configure extension on builtin extendable embedded entities.
	 *
	 * The key must be the name (as defined in [ExtendableEntityName]) of an entity that is used, directly
	 * or indirectly, as a standard property on this root entity.
	 *
	 * Similarly to [objectDefinitionReference], the value is both a reference to an object definition that specifies
	 * extension properties, and the name of the type in the generated custom SDK.
	 *
	 * Each referenced object definition must have a [ObjectDefinition.baseEntity] that is equal to the builtin
	 * embedded entity being extended.
	 */
	val embeddedEntitiesConfigs: Map<ExtendableEntityName, String> = emptyMap(),
)

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class StandardRootEntitiesExtensionConfig(
	override val patient: StandardRootEntityExtensionConfig? = null,
) : ExtendableRootEntitiesConfiguration<StandardRootEntityExtensionConfig>