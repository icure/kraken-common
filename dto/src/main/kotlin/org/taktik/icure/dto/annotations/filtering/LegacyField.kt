package org.taktik.icure.dto.annotations.filtering

/**
 * Marks a field as legacy and the filter generator will add it to the Jackson Filter in the object mapper.
 * This field will be serialized only if the user does not specify an SDK version header or if the user provides the
 * compatibility header.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class LegacyField(val comment: String = "")
