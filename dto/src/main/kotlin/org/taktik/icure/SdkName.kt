package org.taktik.icure

/**
 * This annotation is only used for the generation of entities for the multiplatform SDK.
 * When put on a field specifies the name that the SDK model should use for that field.
 * Normally should exist alongside the JsonAlias annotation.
 *
 * Example:
 *
 * ```kotlin
 * data class Foo(
 *   @SdkName("newName")
 *   @JsonAlias("newName")
 *   val oldName: String
 * )
 * ```
 *
 * - Using oldName as property name will ensure that existing SDK versions will still work (the kraken will serialize the value as oldName).
 * - Using SdkName annotation will ensure that new SDK versions will use newName as the property name.
 * - Using JsonAlias will ensure that kraken can deserialize data coming from new SDK versions.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class SdkName(val name: String)
