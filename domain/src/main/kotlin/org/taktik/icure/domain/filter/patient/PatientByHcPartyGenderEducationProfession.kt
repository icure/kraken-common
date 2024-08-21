/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.patient

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.embed.Gender

/**
 * Retrieves all the [Patient.id]s with a delegation for [healthcarePartyId] where [Patient.gender] is equal to
 * [gender], [Patient.education] is equal to [education], and [Patient.profession] is equal to [profession].
 * Note: if [gender] is null, then the [education] and [profession] parameters will have no effect and if the
 * [education] parameter is null, then the [profession] parameter will have no effect.
 * As this filter explicitly specifies a data owner id, it does not require any security precondition to be used.
 */
interface PatientByHcPartyGenderEducationProfession : Filter<String, Patient> {
	val gender: Gender?
	val education: String?
	val profession: String?
	val healthcarePartyId: String?
}
