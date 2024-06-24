package org.taktik.icure

/**
 * This annotation is only used for the generation of entities for the multiplatform. When put on a field, signifies that the type of the
 * field should always be encrypted in the generated entity (as opposed to always decrypted).
 * This behaviour will not apply to the generated interfaces
 */
@Target(AnnotationTarget.PROPERTY)
annotation class AlwaysEncrypted
