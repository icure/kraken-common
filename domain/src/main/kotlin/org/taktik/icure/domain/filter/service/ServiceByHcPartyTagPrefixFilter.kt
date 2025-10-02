/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.service

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.embed.Service

/**
 * Retrieves all the [Service]s where the following conditions are true:
 * - there is a delegation for [healthcarePartyId]
 * - the service has a tag with type [tagType] and code starting with [tagCodePrefix]
 *
 * This filter is unsorted: the order of the data returned is not guaranteed to follow any special logic, and may change
 * in the future.
 */
interface ServiceByHcPartyTagPrefixFilter : Filter<String, Service> {
	val healthcarePartyId: String
	val tagType: String
	val tagCodePrefix: String
}
