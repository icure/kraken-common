/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.service

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.embed.Service

/**
 * Equivalent to [ServiceByHcPartyPatientTagPrefixFilter] but searches for codes instead of tags.
 */
interface ServiceByHcPartyPatientCodePrefixFilter : Filter<String, Service> {
	val healthcarePartyId: String
	val patientSecretForeignKeys: Set<String>
	val codeType: String
	val codeCodePrefix: String
	val startValueDate: Long?
	val endValueDate: Long?
}
