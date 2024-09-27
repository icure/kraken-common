package org.taktik.icure.asynclogic.impl.filter.contact

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.contact.ContactByDataOwnerOpeningDateFilter
import org.taktik.icure.entities.Contact
import org.taktik.icure.utils.mergeUniqueIdsForSearchKeys

@Service
@Profile("app")
class ContactByDataOwnerOpeningDateFilter(
	private val contactDAO: ContactDAO,
	private val sessionInformationProvider: SessionInformationProvider
) : Filter<String, Contact, ContactByDataOwnerOpeningDateFilter> {

	override fun resolve(
		filter: ContactByDataOwnerOpeningDateFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = flow {
		val searchKeys = sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId)
		mergeUniqueIdsForSearchKeys(searchKeys) { key ->
			contactDAO.listContactIdsByOpeningDate(
				datastoreInformation = datastoreInformation,
				hcPartyId = key,
				startOpeningDate = filter.startDate,
				endOpeningDate = filter.endDate,
				descending = filter.descending ?: false
			)
		}.also { emitAll(it) }
	}

}