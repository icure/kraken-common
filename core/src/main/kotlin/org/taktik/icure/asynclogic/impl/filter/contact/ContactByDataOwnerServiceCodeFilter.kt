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
import org.taktik.icure.domain.filter.contact.ContactByDataOwnerServiceCodeFilter
import org.taktik.icure.entities.Contact
import org.taktik.icure.utils.mergeUniqueIdsForSearchKeys

@Service
@Profile("app")
data class ContactByDataOwnerServiceCodeFilter(
	private val contactDAO: ContactDAO,
	private val sessionInformationProvider: SessionInformationProvider,
) : Filter<String, Contact, ContactByDataOwnerServiceCodeFilter> {
	override fun resolve(
		filter: ContactByDataOwnerServiceCodeFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = flow {
		val searchKeys = sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId)
		mergeUniqueIdsForSearchKeys(searchKeys) { key ->
			contactDAO.listContactIdsByServiceCode(
				datastoreInformation = datastoreInformation,
				hcPartyId = key,
				codeType = filter.codeType,
				codeCode = filter.codeCode,
			)
		}.also { emitAll(it) }
	}
}
