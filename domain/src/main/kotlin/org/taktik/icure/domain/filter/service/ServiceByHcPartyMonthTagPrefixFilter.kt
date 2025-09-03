/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.service

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.embed.Service

/**
 * Retrieves all the [Service]s where the following conditions are true:
 * - there is a delegation for [healthcarePartyId]
 * - the value date (or opening date as fallback) of the service matches the provided [year] and [month].
 *   A month of 0 can be used to match services where the year is known but not the month.
 *   You can use both [year] and [month] `null` to match services where the value date does not exist or is not a fuzzy date time with valid year and month.
 * - the service has a tag with type [tagType] and code starting with [tagCodePrefix]
 * - if [startValueDate] is provided then the value date (or opening date as fallback) must be present and greater than or equal to it
 * - if [endValueDate] is provided then the value date (or opening date as fallback) must be present and less than or equal to it
 *
 * This filter is unsorted: the order of the data returned is not guaranteed to follow any special logic, and may change
 * in the future.
 *
 * The filtering done by [startValueDate] and [endValueDate] is done on the application server side and not on the DB
 * side.
 * If there are too many services matching the other filter values, the query will take long, even if only few services
 * match the value date range.
 */
interface ServiceByHcPartyMonthTagPrefixFilter : Filter<String, Service> {
	val healthcarePartyId: String
	val year: Int?
	val month: Int?
	val tagType: String
	val tagCodePrefix: String
	val startValueDate: Long?
	val endValueDate: Long?
}
