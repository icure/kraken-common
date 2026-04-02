package org.taktik.icure.jackson.annotations

import kotlin.reflect.KClass

@OptionalExpectation
internal expect annotation class JsonInclude(
	val value: JsonIncludeValue = JsonIncludeValue.ALWAYS,
	val content: JsonIncludeValue = JsonIncludeValue.ALWAYS,
	val valueFilter: KClass<*> = PlatformVoid::class,
	val contentFilter: KClass<*> = PlatformVoid::class,
)

internal expect class PlatformVoid

internal expect enum class JsonIncludeValue {
	ALWAYS,
	NON_NULL,
	NON_EMPTY,
	NON_DEFAULT
}
