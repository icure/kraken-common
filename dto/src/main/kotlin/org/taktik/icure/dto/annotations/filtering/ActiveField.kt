package org.taktik.icure.dto.annotations.filtering

/**
 * Marks a field of a DTO as active and so needing no filtering either in the new or legacy versions of the SDK.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class ActiveField
