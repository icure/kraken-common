package org.taktik.icure.utils

import java.time.ZoneId

object Validation {
	fun validZoneId(zoneId: String): Boolean =
		try {
			ZoneId.of(zoneId)
			true
		} catch (_: Exception) {
			false
		}
}
