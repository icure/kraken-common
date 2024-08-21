/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.patient

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Patient

/**
 * Retrieves all the [Patient.id]s with a delegation for [healthcarePartyId] where [Patient.ssin] starts with the
 * provided [ssin].
 * As this filter explicitly specifies a data owner id, it does not require any security precondition to be used.
 */
interface PatientByHcPartyAndSsinFilter : Filter<String, Patient> {
	val ssin: String
	val healthcarePartyId: String?
}
