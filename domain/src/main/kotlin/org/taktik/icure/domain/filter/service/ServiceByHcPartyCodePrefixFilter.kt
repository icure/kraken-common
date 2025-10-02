/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.service

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.embed.Service

/**
 * Equivalent to [ServiceByHcPartyTagPrefixFilter] but searches for codes instead of tags.
 */
interface ServiceByHcPartyCodePrefixFilter : Filter<String, Service> {
	val healthcarePartyId: String
	val codeType: String
	val codeCodePrefix: String
}
