package org.taktik.icure

/**
 * Used to indicate a builtin type that should be exposed for use in custom entities even if not extendable.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ExposedToCustomEntities(
	/**
	 * If the entity should be specializable
	 */
	val specializable: Boolean = false
)
