/*
 * Copyright (c) 2026. Taktik SA, All rights reserved.
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
import org.taktik.icure.domain.filter.VersionFiltering
import org.taktik.icure.domain.filter.service.ServiceByDataOwnerCodeCodesFilter
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.utils.FuzzyDates
import org.taktik.icure.utils.FuzzyDates.maxPossibleFuzzyNowInAllTimeZones

@org.springframework.stereotype.Service
@Profile("app")
class ServiceByDataOwnerCodeCodesFilter(
	private val contactDAO: ContactDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, Service, ServiceByDataOwnerCodeCodesFilter> {
	companion object {
		private const val MAX_MONTHS = 24
	}

	override val entity get() = contactDAO.entityClass
	override val views = listOf("service_by_all_delegates_month_code_prefix", "service_by_all_delegates_code_prefix")

	override fun resolve(
		filter: ServiceByDataOwnerCodeCodesFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		val startValueDate = filter.startValueDate
		val endValueDate = filter.endValueDate
		val filterVersion = filter.filterVersion ?: VersionFiltering.ANY
		val searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId)

		val monthRange = if (startValueDate != null) {
			FuzzyDates.getMonthRange(startValueDate, endValueDate ?: maxPossibleFuzzyNowInAllTimeZones(), MAX_MONTHS)
		} else null

		if (monthRange != null) {
			val emitted = mutableSetOf<String>()
			monthRange.forEachIndexed { index, (year, month) ->
				val isFirst = index == 0
				val isLast = index == monthRange.lastIndex
				contactDAO.listServiceIdsByDataOwnerValueDateMonthCodeCodes(
					datastoreInformation = datastoreInformation,
					searchKeys = searchKeys,
					year = year,
					month = month,
					codeTypesAndCodes = filter.codeCodes,
					startValueDate = if (isFirst) startValueDate else null,
					endValueDate = if (isLast) endValueDate else null,
					filterVersion = filterVersion,
				).collect { serviceId ->
					if (emitted.add(serviceId)) {
						emit(serviceId)
					}
				}
			}
		} else {
			emitAll(contactDAO.listServiceIdsByDataOwnerCodeCodes(
				datastoreInformation = datastoreInformation,
				searchKeys = searchKeys,
				codeTypesAndCodes = filter.codeCodes,
				startValueDate = startValueDate,
				endValueDate = endValueDate,
				filterVersion = filterVersion,
			))
		}
	}
}
