/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.service

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.embed.Service

/**
 * Retrieves all the [Service.id]s with a delegation for [healthcarePartyId] and associated to
 * the patient given the [patientSecretForeignKeys].
 * As this filter explicitly specifies a data owner id, it does not require any security precondition to be used.
 */
interface ServiceBySecretForeignKeys : Filter<String, Service> {
	val healthcarePartyId: String?
	val patientSecretForeignKeys: Set<String>
}
