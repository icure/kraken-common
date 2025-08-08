package org.taktik.icure.asynclogic.impl.filter.contact

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.contact.ContactByDataOwnerServiceTagFilter
import org.taktik.icure.entities.Contact
import org.taktik.icure.utils.mergeUniqueIdsForSearchKeys

@Service
@Profile("app")
class ContactByDataOwnerServiceTagFilter(
	private val contactDAO: ContactDAO,
	private val sessionInformationProvider: SessionInformationProvider,
) : Filter<String, Contact, ContactByDataOwnerServiceTagFilter> {
	override fun resolve(
		filter: ContactByDataOwnerServiceTagFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = flow {
		val searchKeys = sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId)
		mergeUniqueIdsForSearchKeys(searchKeys) { key ->
			contactDAO.listContactIdsByServiceTag(
				datastoreInformation = datastoreInformation,
				hcPartyId = key,
				tagType = filter.tagType,
				tagCode = filter.tagCode,
			)
		}.also { emitAll(it) }
	}
}
