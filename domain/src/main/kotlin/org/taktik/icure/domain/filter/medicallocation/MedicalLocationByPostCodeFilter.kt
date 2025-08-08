package org.taktik.icure.domain.filter.medicallocation

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.MedicalLocation

/**
 * Retrieves all the [MedicalLocation]s where the post code in [MedicalLocation.address] is equal to [postCode].
 * This filter requires a special permission to be used.
 */
interface MedicalLocationByPostCodeFilter : Filter<String, MedicalLocation> {
	val postCode: String
}
