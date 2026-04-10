/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.service

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.embed.Service

/**
 * Equivalent to [ServiceByHcPartyCodePrefixFilter] but matches exact code codes instead of prefixes.
 */
interface ServiceByHcPartyCodesFilter : Filter<String, Service> {
	val healthcarePartyId: String
	val codeCodes: Map<String, Collection<String>>
	val startValueDate: Long?
	val endValueDate: Long?
}
