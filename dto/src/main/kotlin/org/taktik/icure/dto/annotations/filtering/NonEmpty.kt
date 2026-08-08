package org.taktik.icure.dto.annotations.filtering

/**
 * To be used only on collection and map fields. Specifies that the jackson JsonInclude NON_EMPTY strategy will be used
 * to serialize this field.
 */
@Target()
@Retention(AnnotationRetention.SOURCE)
annotation class NonEmpty
