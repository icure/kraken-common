package org.taktik.icure.domain.filter.hcparty

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.HealthcareParty

/**
 * Retrieves all the [HealthcareParty.id]s where [HealthcareParty.ssin], [HealthcareParty.nihii],
 * [HealthcareParty.cbe], or [HealthcareParty.ehp] start with [searchValue].
 * The results will be sorted lexicographically by these identifiers in ascending or descending order according to the
 * [descending] parameter.
 * A special permission is required to use this filter.
 */
interface HealthcarePartyByNationalIdentifierFilter : Filter<String, HealthcareParty> {
	val searchValue: String
	val descending: Boolean?
}