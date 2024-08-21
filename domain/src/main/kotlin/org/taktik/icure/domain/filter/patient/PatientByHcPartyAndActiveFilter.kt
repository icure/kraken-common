/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.patient

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Patient

/**
 * Retrieves all the [Patient]s with a delegation for [healthcarePartyId] where [Patient.active] is [active].
 * As this filter explicitly specifies a data owner id, it does not require any security precondition to be used.
 */
interface PatientByHcPartyAndActiveFilter : Filter<String, Patient> {
	val active: Boolean
	val healthcarePartyId: String?
}
