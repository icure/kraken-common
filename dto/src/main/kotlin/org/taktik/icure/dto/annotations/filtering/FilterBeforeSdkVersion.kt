package org.taktik.icure.dto.annotations.filtering

/**
 * Marks a field as introduced in a specific version of the SDK and the filter generator will add it to the Jackson Filter in the object mapper.
 * This field will not be serialized if the user is using an SDK version that is strictly less than the specified [version].
 */
@Target(AnnotationTarget.PROPERTY)
annotation class FilterBeforeSdkVersion(val version: String)
