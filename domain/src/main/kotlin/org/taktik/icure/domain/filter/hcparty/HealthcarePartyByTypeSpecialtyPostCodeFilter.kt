package org.taktik.icure.domain.filter.hcparty

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.HealthcareParty

/**
 * Retrieves all the [HealthcareParty] entities in a group where the [HealthcareParty.speciality] is equal to [specialty],
 * [HealthcareParty.nihiiSpecCode] is equal to [specCode], and they have a postal code in [HealthcareParty.addresses] that
 * is between [startPostCode] and [endPostCode].
 * A special permission is required to use this filter.
 */
interface HealthcarePartyByTypeSpecialtyPostCodeFilter : Filter<String, HealthcareParty> {
	val specialty: String
	val specCode: String
	val startPostCode: String
	val endPostCode: String
}
