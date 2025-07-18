package org.taktik.icure.entities

/**
 * Represents the type of data owner.
 */
enum class DataOwnerType {
	HCP,
	DEVICE,
	PATIENT,
	;

	companion object {
		fun valueOfOrNullCaseInsensitive(stringValue: String): DataOwnerType? = when (stringValue.uppercase()) {
			"HCP" -> HCP
			"DEVICE" -> DEVICE
			"PATIENT" -> PATIENT
			else -> null
		}
	}
}
