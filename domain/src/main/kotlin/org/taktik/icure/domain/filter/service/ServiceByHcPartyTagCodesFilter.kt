/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.service

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.embed.Service

/**
 * Equivalent to [ServiceByHcPartyTagPrefixFilter] but matches exact tag codes instead of prefixes.
 */
interface ServiceByHcPartyTagCodesFilter : Filter<String, Service> {
	val healthcarePartyId: String
	val tagCodes: Map<String, Collection<String>>
	val startValueDate: Long?
	val endValueDate: Long?
}
