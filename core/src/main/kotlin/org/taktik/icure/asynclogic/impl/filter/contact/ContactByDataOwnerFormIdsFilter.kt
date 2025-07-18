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
import org.taktik.icure.domain.filter.contact.ContactByDataOwnerFormIdsFilter
import org.taktik.icure.entities.Contact

@Service
@Profile("app")
class ContactByDataOwnerFormIdsFilter(
	private val contactDAO: ContactDAO,
	private val sessionInformationProvider: SessionInformationProvider,
) : Filter<String, Contact, ContactByDataOwnerFormIdsFilter> {
	override fun resolve(
		filter: ContactByDataOwnerFormIdsFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = flow {
		contactDAO
			.listContactIdsByDataOwnerAndFormIds(
				datastoreInformation = datastoreInformation,
				searchKeys = sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId),
				formIds = filter.formIds,
			).also { emitAll(it) }
	}
}
