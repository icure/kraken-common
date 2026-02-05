package org.taktik.icure.domain.customentities.config

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class StandardRootEntityExtensionConfig(
	/**
	 * This value is used for 2 purposes:
	 * - Name of the root entity in the generated custom SDK
	 * - Reference to the definition of an object that specifies all the extensions for the configured entity (key of
	 *   [VersionedCustomEntitiesConfiguration.objects]).
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
	 * The key must be the canonical name of an entity that is embedded, directly or indirectly, on this root entity,
	 * either from the standard icure model, or from their extensions on standard icure model entities.
	 *
	 * Similarly to [objectDefinitionReference], the value is both a reference to an object definition that specifies
	 * extension properties, and the name of the type in the generated custom SDK.
	 */
	val embeddedEntitiesConfigs: Map<String, String> = emptyMap(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class StandardRootEntitiesExtensionConfig(
	override val patient: StandardRootEntityExtensionConfig? = null,
) : ExtendableRootEntitiesConfiguration<StandardRootEntityExtensionConfig>