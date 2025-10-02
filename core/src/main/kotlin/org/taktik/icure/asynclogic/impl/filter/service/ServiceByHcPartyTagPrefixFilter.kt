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
import org.taktik.icure.domain.filter.service.ServiceByHcPartyMonthTagPrefixFilter
import org.taktik.icure.domain.filter.service.ServiceByHcPartyTagPrefixFilter
import org.taktik.icure.entities.embed.Service

@org.springframework.stereotype.Service
@Profile("app")
class ServiceByHcPartyTagPrefixFilter(
	private val contactDAO: ContactDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, Service, ServiceByHcPartyTagPrefixFilter> {
	override fun resolve(
		filter: ServiceByHcPartyTagPrefixFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		emitAll(contactDAO.listServiceIdsByDataOwnerTagCodePrefix(
			datastoreInformation = datastoreInformation,
			searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(filter.healthcarePartyId),
			tagType = filter.tagType,
			tagCodePrefix = filter.tagCodePrefix,
		))
	}
}
