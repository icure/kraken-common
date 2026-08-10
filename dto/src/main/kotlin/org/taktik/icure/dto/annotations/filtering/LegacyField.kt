package org.taktik.icure.dto.annotations.filtering

/**
 * This field will be serialized only if the user does not specify an SDK version header or if the user provides the
 * compatibility header.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class LegacyField(val comment: String = "")
