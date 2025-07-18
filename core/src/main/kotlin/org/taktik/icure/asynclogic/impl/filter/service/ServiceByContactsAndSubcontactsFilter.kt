/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.service.ServiceByContactsAndSubcontactsFilter

@Service
@Profile("app")
class ServiceByContactsAndSubcontactsFilter(
	private val contactDAO: ContactDAO,
) : Filter<String, org.taktik.icure.entities.embed.Service, ServiceByContactsAndSubcontactsFilter> {
	@OptIn(ExperimentalCoroutinesApi::class)
	override fun resolve(
		filter: ServiceByContactsAndSubcontactsFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> {
		val contacts = contactDAO.getContacts(datastoreInformation, filter.contacts)
		return if (filter.subContacts != null) {
			contacts.flatMapConcat { c ->
				c.subContacts
					.flatMap { sc ->
						if (filter.subContacts!!.contains(sc.id)) sc.services.mapNotNull { it.serviceId } else listOf()
					}.asFlow()
			}
		} else {
			contacts.flatMapConcat { c -> c.services.map { it.id }.asFlow() }
		}
	}
}
