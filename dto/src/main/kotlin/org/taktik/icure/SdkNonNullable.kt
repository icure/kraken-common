package org.taktik.icure

/**
 * This annotation is to be set on fields that are nullable on the dto but must not be nullable on the generated SDK
 * model.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class SdkNonNullable