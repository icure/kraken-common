/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.patient

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Patient

/**
 * Retrieves all the [Patient.id]s with a delegation for [healthcarePartyId] where [Patient.dateOfBirth] is between
 * [maxDateOfBirth] (if provided) and [minDateOfBirth] (if provided).
 * If the fuzzy date [minDateOfBirth] is null, then a lower bound for [Patient.dateOfBirth] will not be set.
 * If the fuzzy date [maxDateOfBirth] is null, then an upper bound for [Patient.dateOfBirth] will not be set.
 * As this filter explicitly specifies a data owner id, it does not require any security precondition to be used.
 */
interface PatientByHcPartyDateOfBirthBetweenFilter : Filter<String, Patient> {
	val maxDateOfBirth: Int?
	val minDateOfBirth: Int?
	val healthcarePartyId: String?
}
