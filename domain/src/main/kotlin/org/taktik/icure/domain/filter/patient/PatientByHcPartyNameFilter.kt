/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.patient

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Patient

/**
 * Returns all the [Patient.id]s with a delegation for [healthcarePartyId] where [Patient.firstName], [Patient.lastName],
 * [Patient.maidenName], or [Patient.spouseName] start with the specified [name].
 * As this filter explicitly specifies a data owner id, it does not require any security precondition to be used.
 */
interface PatientByHcPartyNameFilter : Filter<String, Patient> {
	val name: String?
	val healthcarePartyId: String?
}
