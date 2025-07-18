package org.taktik.icure.asynclogic.impl.filter.contact

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.contact.ContactByExternalIdFilter
import org.taktik.icure.entities.Contact

@Service
@Profile("app")
class ContactByExternalIdFilter(
	private val contactDAO: ContactDAO,
) : Filter<String, Contact, ContactByExternalIdFilter> {
	override fun resolve(
		filter: ContactByExternalIdFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = contactDAO.listContactIdsByExternalId(
		datastoreInformation = datastoreInformation,
		externalId = filter.externalId,
	)
}
