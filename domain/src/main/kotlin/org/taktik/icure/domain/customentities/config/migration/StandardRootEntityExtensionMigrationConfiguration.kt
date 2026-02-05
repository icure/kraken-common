package org.taktik.icure.domain.customentities.config.migration

import org.taktik.icure.domain.customentities.config.ExtendableRootEntitiesConfiguration
import org.taktik.icure.domain.customentities.config.StandardRootEntityExtensionConfig

data class StandardRootEntityExtensionMigrationConfiguration(
	/**
	 * Specifies how to initialize the extension on this root entity.
	 * Must be null if the entity root doesn't have a new extension (but a configuration is still required to initialize
	 * the extensions on embedded entities).
	 */
	val rootExtensionInitialization: ExtensionInitialization? = null,
	/**
	 * Specifies how to initialize extensions on builtin embedded entities.
	 *
	 * The key must match a key in the target configuration [StandardRootEntityExtensionConfig.embeddedEntitiesConfigs]
	 * matching this root, for which there was no extension in the source configuration.
	 */
	val embeddedExtensionInitializations: Map<String, ExtensionInitialization> = emptyMap(),
	// Changed plan: going to always emit a warning and the cockpit that can decide to show it as an error
//	/**
//	 * If true, indicates that the extension on this root entity existed in the source configuration but has been
//	 * intentionally removed in the target configuration.
//	 * If the source configuration had an extension on this root entity, but the target configuration doesn't, and this
//	 * is not true, an error will be raised.
//	 * If true and the root extension was not deleted, a warning will be raised.
//	 */
//	val removedRootExtension: Boolean = false,
//	/**
//	 * Keys of embedded extensions that existed in the source configuration but have been intentionally removed in the
//	 * target configuration.
//	 * If the source configuration had an extension on a builtin embedded entity, but the target configuration doesn't,
//	 * and the key is not in this set, an error will be raised.
//	 * If a key is in this set but the corresponding embedded extension was not deleted, a warning will be raised.
//	 */
//	val removedEmbeddedExtensions: Set<String> = emptySet(),
)

data class StandardRootEntitiesExtensionMigrationConfiguration(
	override val patient: StandardRootEntityExtensionMigrationConfiguration? = null,
) : ExtendableRootEntitiesConfiguration<StandardRootEntityExtensionMigrationConfiguration>