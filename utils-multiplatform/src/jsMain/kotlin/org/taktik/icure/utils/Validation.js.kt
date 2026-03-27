package org.taktik.icure.utils

import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZone

actual object Validation {
	actual fun validZoneId(zoneId: String): Boolean = try {
		TimeZone.of(zoneId)
		true
	} catch (_: IllegalTimeZoneException) {
		false
	}
}