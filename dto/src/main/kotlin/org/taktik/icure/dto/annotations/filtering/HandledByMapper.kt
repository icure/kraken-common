package org.taktik.icure.dto.annotations.filtering

/**
 * Marks a field of a DTO that even though is not active it is being handled by the mapper, so there is no need to
 * generate a filter
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class HandledByMapper
