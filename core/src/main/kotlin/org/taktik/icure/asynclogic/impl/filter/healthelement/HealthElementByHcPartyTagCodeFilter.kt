/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl.filter.healthelement

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toSet
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.HealthElementDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.healthelement.HealthElementByHcPartyTagCodeFilter
import org.taktik.icure.entities.HealthElement
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class HealthElementByHcPartyTagCodeFilter(
	private val healthElementDAO: HealthElementDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, HealthElement, HealthElementByHcPartyTagCodeFilter> {
	override fun resolve(
		filter: HealthElementByHcPartyTagCodeFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		try {
			val hcPartyId = filter.healthcarePartyId
			val searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(hcPartyId)
			val searchByTag = filter.tagType != null && filter.tagCode != null
			val searchByCode = filter.codeType != null && filter.codeCode != null

			val ids = mutableSetOf<String>()
			if (searchByTag) {
				ids.addAll(
					healthElementDAO
						.listHealthElementIdsByHcPartyAndTags(
							datastoreInformation = datastoreInformation,
							searchKeys = searchKeys,
							tagType = filter.tagType!!,
							tagCode = filter.tagCode!!,
						).toSet(),
				)
			}
			if (filter.codeType != null && filter.codeCode != null) {
				val byCode =
					healthElementDAO
						.listHealthElementIdsByHcPartyAndCodes(
							datastoreInformation = datastoreInformation,
							searchKeys = searchKeys,
							codeType = filter.codeType!!,
							codeCode = filter.codeCode!!,
						).toSet()
				if (!searchByTag) {
					ids.addAll(byCode)
				} else {
					ids.retainAll(byCode)
				}
			}
			if (filter.status != null) {
				val byStatus =
					healthElementDAO
						.listHealthElementIdsByHcPartyAndStatus(
							datastoreInformation = datastoreInformation,
							searchKeys = searchKeys,
							status = filter.status!!,
						).toSet()
				if (!searchByTag && !searchByCode) {
					ids.addAll(byStatus)
				} else {
					ids.retainAll(byStatus)
				}
			}
			ids.forEach { emit(it) }
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
