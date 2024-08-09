package org.taktik.icure.domain.filter.patient

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Patient

/**
 * Retrieves all the [Patient.id]s with a delegation a data owner (through their data owner id + access control keys),
 * where [Patient.modified] is not null and greater than or equal to[startDate] (if provided, otherwise not lower
 * bound will be set), and less than or equal to [endDate] (if provided, otherwise no upper bound will be set).
 * If the [Patient.modified] timestamp is null, then [Patient.created] will be considered. If also [Patient.created]
 * is null, then this patient will be considered in the results.
 * The results will be sorted by [Patient.modified] or [Patient.created] in ascending or descending order according
 * to the [descending] parameter.
 * As this filter explicitly specifies a data owner id, it does not require any security precondition to be used.
 */
interface PatientByDataOwnerModifiedAfterFilter : Filter<String, Patient> {
	val dataOwnerId: String
	val startDate: Long?
	val endDate: Long?
	val descending: Boolean?
}