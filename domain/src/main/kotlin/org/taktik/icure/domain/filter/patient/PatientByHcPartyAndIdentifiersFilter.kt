package org.taktik.icure.domain.filter.patient

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.embed.Identifier

/**
 * Retrieves all the [Patient]s with a delegation to [healthcarePartyId] and that have at least
 * one of the provided [identifiers] in [Patient.identifier].
 * As this filter explicitly specifies a data owner id, it does not require any security precondition to be used.
 */
interface PatientByHcPartyAndIdentifiersFilter : Filter<String, Patient> {
	val healthcarePartyId: String?
	val identifiers: List<Identifier>
}
