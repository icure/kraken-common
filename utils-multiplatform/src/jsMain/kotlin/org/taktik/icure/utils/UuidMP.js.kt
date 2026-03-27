package org.taktik.icure.utils

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

actual object UuidMP {
	@OptIn(ExperimentalUuidApi::class)
	actual fun randomCryptoSafeUuidString(): String = Uuid.random().toString()
}