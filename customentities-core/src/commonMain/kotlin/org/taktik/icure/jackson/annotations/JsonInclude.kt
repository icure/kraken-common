package org.taktik.icure.jackson.annotations

import kotlin.reflect.KClass

@OptionalExpectation
internal expect annotation class JsonInclude(
	val value: Include = Include.ALWAYS,
	val content: Include = Include.ALWAYS,
	val valueFilter: KClass<*> = PlatformVoid::class,
	val contentFilter: KClass<*> = PlatformVoid::class,
)

internal expect class PlatformVoid

internal expect enum class Include {
	ALWAYS,
	NON_NULL,
	NON_EMPTY,
	NON_DEFAULT
}
