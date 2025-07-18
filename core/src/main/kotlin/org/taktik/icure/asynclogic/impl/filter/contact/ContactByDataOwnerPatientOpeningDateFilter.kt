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
import org.taktik.icure.domain.filter.contact.ContactByDataOwnerPatientOpeningDateFilter
import org.taktik.icure.entities.Contact

@Service
@Profile("app")
class ContactByDataOwnerPatientOpeningDateFilter(
	private val contactDAO: ContactDAO,
	private val sessionInformationProvider: SessionInformationProvider,
) : Filter<String, Contact, ContactByDataOwnerPatientOpeningDateFilter> {
	override fun resolve(
		filter: ContactByDataOwnerPatientOpeningDateFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = flow {
		contactDAO
			.listContactIdsByDataOwnerPatientOpeningDate(
				datastoreInformation = datastoreInformation,
				searchKeys = sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId),
				secretForeignKeys = filter.secretForeignKeys,
				startDate = filter.startDate,
				endDate = filter.endDate,
				descending = filter.descending ?: false,
			).also { emitAll(it) }
	}
}
