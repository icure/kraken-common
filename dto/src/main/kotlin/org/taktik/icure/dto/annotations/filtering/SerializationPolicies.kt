package org.taktik.icure.dto.annotations.filtering

import kotlin.reflect.KClass

@Target()
@Retention(AnnotationRetention.SOURCE)
annotation class Since(
	val version: String,
	val strategy: KClass<out Annotation>,
)

/**
 * Represents the serialization policy of a field across multiple versions of the sdk. Needs a list of [Since]: each one
 * of those specifies the strategy that will be used to serialize each field when the version of the SDK provided by the user is greater
 * or equal than the one specified. e.g.:
 * ```
 * @SerializationPolicy(Since("2.1.0", Filtered::class), Since("2.2.0", NonEmpty::class))
 * ```
 * This means that if the user's SDK version is >= 2.1.0 and <2.2.0 then the field will be filtered out. If the user's
 * SDK version is >= 2.0.0 then the field is serialized using the JacksonInclude NON_EMPTY strategy. Otherwise, the field
 * will be serialized normally.
 * In general, when no version header is passed, all the fields are always serialized with the default Jackson strategy
 * defined on the class or on the field.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class SerializationPolicy(vararg val rules: Since)
