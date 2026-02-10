package org.taktik.icure.domain.customentities.config

/**
 * All extendable builtin entities
 */
enum class ExtendableEntityName {
	Address, // org.taktik.icure.entities.embed.Address
	Patient; // org.taktik.icure.entities.Patient

	/**
	 * Need this for use in annotation, should be able to remove in future with https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0444-improve-compile-time-constants.md
	 */
	companion object {
		const val AddressName = "Address"
		const val PatientName = "Patient"
	}
}