package org.taktik.icure

/**
 * Used to indicate properties that are used to keep metadata information of particular importance to cardinal on an
 * entity (access control, encryption support, ...).
 *
 * In the generation of builtin entities definitions for integration with custom entities this annotation is used to
 * mark fields that should be hidden from the definition, to prevent from using that field in a migration.
 *
 * This annotation also prevents the annotated fields to be used freely in the definition of custom views: it can only
 * be used if a built-in key element is defined for the field. The available built-in key elements must be declared in the
 * customViewFields of the annotation.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class CardinalMetadataProperty(
	vararg val customViewFields: ViewBuiltInField
)
