/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter.contact

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.contact.ContactByServiceIdsFilter
import org.taktik.icure.entities.Contact

@Service
@Profile("app")
class ContactByServiceIdsFilter(
	private val contactDAO: ContactDAO
) : Filter<String, Contact, ContactByServiceIdsFilter> {

	override fun resolve(
        filter: ContactByServiceIdsFilter,
        context: Filters,
        datastoreInformation: IDatastoreInformation
    ): Flow<String> = filter.ids?.let {
		contactDAO.listIdsByServices(datastoreInformation, it)
	}?.map { it.contactId } ?: flowOf()
}
