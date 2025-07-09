package org.taktik.icure

/**
 * This annotation is only used for the generation of entities for the multiplatform. When put on a field, signifies that
 * the type of the field should always be decrypted in the generated encryptable entities (both interface and annotations).
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class AlwaysDecrypted

