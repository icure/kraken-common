package org.taktik.icure.customentities.util

import org.taktik.icure.customentities.config.migration.EnumMigration
import org.taktik.icure.customentities.config.typing.ObjectDefinition

interface BuiltinDefinitionsProvider {
	fun getBuiltinEnumDefinition(name: String): BuiltinEnumDefinition?
	fun getBuiltinObjectDefinition(name: String): BuiltinObjectDefinition?

	data class BuiltinEnumDefinition(
		val entries: Set<Entry>
	) {
		/**
		 * An entry of a builtin enum definition.
		 * Unlike custom enum definition there might be a distinction between serial name of an entry and the name
		 * displayed to a user.
		 */
		data class Entry(
			/**
			 * The user-facing name of the entry, what it is used in the SDK code and displayed by the cockpit UI.
			 * This is the primary value used in migration-related validation:
			 * - Value considered for [EnumMigration.automaticallyMapIdenticalNames]
			 * - Value expected in the keys of [EnumMigration.sourceMappings] for builtin enums
			 * - Value considered for type coercion
			 */
			val sdkName: String,
			/**
			 * The value used when the entry is serialized for the rest methods and for storage in the db.
			 * This is the primary value used in value-related validation:
			 * - Value considered for validating an object property default value
			 * - Value considered for validating an enum property value inside a custom type
			 */
			val serialName: String
		)
	}

	data class BuiltinObjectDefinition(
		/**
		 * Properties of the builtin object, excluding metadata properties
		 */
		val properties: Map<String, ObjectDefinition.PropertyConfiguration>,
		/**
		 * If this object definition represens an entity that can have custom extensions added to it
		 */
		val isExtendable: Boolean,
		/**
		 * If this object definition represents an extendable entity that is a root entity in the database.
		 * These entities can't be embedded in other entities
		 */
		val isRoot: Boolean,
		/**
		 * Name of properties that are included in [properties] but have been deprecated.
		 * They might still be useful in migrations.
		 */
		val deprecatedProperties: Set<String>,
		/**
		 * Name of properties that are not included in the [properties] because they are holding metadata that has
		 * particular significance to cardinal (for access control, encryption, ...) and therefore should not be
		 * customized or used in a migration, otherwise some processes may break.
		 *
		 * Used only to support providing better error messages.
		 */
		val metadataProperties: Set<String>
	)
}

/**
 * Only to use when using a validated configuration, in case of illegal manipulation of the configuration that causes
 * a mandatory reference to be missing throws an IllegalEntityException.
 * No need to have a ResolutionPath for proper error messages as this should never happen if the configuration was
 * always changed through the appropriate endpoints.
 */
fun BuiltinDefinitionsProvider.getRequiredObjectDefinition(
	reference: String
): BuiltinDefinitionsProvider.BuiltinObjectDefinition = checkNotNull(getBuiltinObjectDefinition(reference)) {
	"Object definition for reference `$reference` not found"
}

/**
 * Only to use when using a validated configuration, in case of illegal manipulation of the configuration that causes
 * a mandatory reference to be missing throws an IllegalEntityException.
 * No need to have a ResolutionPath for proper error messages as this should never happen if the configuration was
 * always changed through the appropriate endpoints.
 */
fun BuiltinDefinitionsProvider.getRequiredEnumDefinition(
	reference: String
): BuiltinDefinitionsProvider.BuiltinEnumDefinition = checkNotNull(getBuiltinEnumDefinition(reference)) {
	"Object definition for reference `$reference` not found"
}