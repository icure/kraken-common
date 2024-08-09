/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.service

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.embed.Service

interface ServiceByContactsAndSubcontactsFilter : Filter<String, Service> {
	val healthcarePartyId: String?
	val contacts: Set<String>
	val subContacts: Set<String>?
	val startValueDate: Long?
	val endValueDate: Long?
}
