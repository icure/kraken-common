/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter.service

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.service.ServiceByHcPartyTagCodesFilter
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.utils.FuzzyDates
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@org.springframework.stereotype.Service
@Profile("app")
class ServiceByHcPartyTagCodesFilter(
	private val contactDAO: ContactDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, Service, ServiceByHcPartyTagCodesFilter> {
	override fun resolve(
		filter: ServiceByHcPartyTagCodesFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		val startValueDate = filter.startValueDate
		val endValueDate = filter.endValueDate
		val searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(filter.healthcarePartyId)

		val monthRange = if (startValueDate != null) {
			FuzzyDates.getMonthRange(startValueDate, endValueDate ?: FuzzyDates.getFuzzyDateTime(LocalDateTime.now(ZoneId.ofOffset("UTC", java.time.ZoneOffset.ofHours(14))),
				ChronoUnit.SECONDS, false))
		} else null

		if (monthRange != null) {
			val emitted = mutableSetOf<String>()
			monthRange.forEachIndexed { index, (year, month) ->
				val isFirst = index == 0
				val isLast = index == monthRange.lastIndex
				contactDAO.listServiceIdsByDataOwnerValueDateMonthTagCodes(
					datastoreInformation = datastoreInformation,
					searchKeys = searchKeys,
					year = year,
					month = month,
					tagTypesAndCodes = filter.tagCodes,
					startValueDate = if (isFirst) startValueDate else null,
					endValueDate = if (isLast) endValueDate else null,
				).collect { serviceId ->
					if (emitted.add(serviceId)) {
						emit(serviceId)
					}
				}
			}
		} else {
			emitAll(contactDAO.listServiceIdsByDataOwnerTagCodes(
				datastoreInformation = datastoreInformation,
				searchKeys = searchKeys,
				tagTypesAndCodes = filter.tagCodes,
				startValueDate = startValueDate,
				endValueDate = endValueDate,
			))
		}
	}
}
