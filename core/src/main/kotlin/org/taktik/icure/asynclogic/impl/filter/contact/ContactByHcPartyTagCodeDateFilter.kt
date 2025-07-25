/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter.contact

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toSet
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.contact.ContactByHcPartyTagCodeDateFilter
import org.taktik.icure.entities.Contact
import org.taktik.icure.utils.getLoggedHealthCarePartyId
import org.taktik.icure.utils.mergeUniqueIdsForSearchKeys
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class ContactByHcPartyTagCodeDateFilter(
	private val contactDAO: ContactDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, Contact, ContactByHcPartyTagCodeDateFilter> {
	override fun resolve(
		filter: ContactByHcPartyTagCodeDateFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		try {
			val searchKeys =
				sessionLogic.getAllSearchKeysIfCurrentDataOwner(
					filter.healthcarePartyId ?: getLoggedHealthCarePartyId(sessionLogic),
				)
			val ids = mutableSetOf<String>()
			if (filter.tagType != null && filter.tagCode != null) {
				val contactIds =
					mergeUniqueIdsForSearchKeys(searchKeys) { key ->
						contactDAO.listContactIdsByTag(
							datastoreInformation,
							key,
							filter.tagType!!,
							filter.tagCode!!,
							filter.startOfContactOpeningDate,
							filter.endOfContactOpeningDate,
						)
					}.toSet()
				ids.addAll(contactIds)
			}
			if (filter.codeType != null && filter.codeCode != null) {
				val byCode =
					mergeUniqueIdsForSearchKeys(searchKeys) { key ->
						contactDAO.listContactIdsByCode(
							datastoreInformation,
							key,
							filter.codeType!!,
							filter.codeCode!!,
							filter.startOfContactOpeningDate,
							filter.endOfContactOpeningDate,
						)
					}.toSet()
				if (ids.isEmpty()) {
					ids.addAll(byCode)
				} else {
					ids.retainAll(byCode)
				}
			}
			emitAll(ids.asFlow())
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
