package org.taktik.icure.utils

actual object UuidMP {
	actual fun randomCryptoSafeUuidString(): String = java.util.UUID.randomUUID().toString()
}