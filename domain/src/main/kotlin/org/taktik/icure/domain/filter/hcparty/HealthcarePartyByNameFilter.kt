package org.taktik.icure.domain.filter.hcparty

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.HealthcareParty

/**
 * Retrieves all the [HealthcareParty.id]s in a group, sorted by the concatenation of [HealthcareParty.lastName] and
 * [HealthcareParty.firstName] normalized removing all the characters that are not letters and mapping all the characters
 * outside the standard english alphabet to letters of the alphabet.
 * Only the [HealthcareParty] where this string starts with the provided [name] will be returned.
 * The results will be sorted lexicographically by [HealthcareParty.lastName] and [HealthcareParty.firstName] in ascending
 * or descending order according to the [descending] parameter.
 * This filter requires a special permission to be used.
 */
interface HealthcarePartyByNameFilter : Filter<String, HealthcareParty> {
	val desc: String?
	val name: String
	val descending: Boolean?
}
