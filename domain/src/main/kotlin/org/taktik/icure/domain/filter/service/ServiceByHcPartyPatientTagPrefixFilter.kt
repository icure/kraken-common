/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.service

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.embed.Service

/**
 * Retrieves all the [Service]s where the following conditions are true:
 * - there is a delegation for [healthcarePartyId]
 * - the sfk of the service's contact includes at least one of [patientSecretForeignKeys]
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
interface ServiceByHcPartyPatientTagPrefixFilter : Filter<String, Service> {
	val healthcarePartyId: String
	val patientSecretForeignKeys: Set<String>
	val tagType: String
	val tagCodePrefix: String
	val startValueDate: Long?
	val endValueDate: Long?
}
