package org.taktik.icure.entities

enum class UserType {
	HCP,
	PATIENT,
	DEVICE,
	USER,
	;

	companion object {
		fun valueOfOrNullCaseInsensitive(stringValue: String): UserType? = when (stringValue.uppercase()) {
			"HCP" -> HCP
			"DEVICE" -> DEVICE
			"PATIENT" -> PATIENT
			"USER" -> USER
			else -> null
		}
	}
}
