package org.taktik.icure

/**
 * Used to indicate properties that are used to keep metadata information of particular importance to cardinal on an
 * entity (access control, encryption support, ...).
 *
 * In the generation of builtin entities definitions for integration with custom entities this annotation is used to
 * mark fields that should be hidden from the definition, to prevent from using that field in a migration.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class CardinalMetadataProperty
