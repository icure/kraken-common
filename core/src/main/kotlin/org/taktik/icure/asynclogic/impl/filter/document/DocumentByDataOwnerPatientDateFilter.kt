package org.taktik.icure.asynclogic.impl.filter.document

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.DocumentDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.document.DocumentByDataOwnerPatientDateFilter
import org.taktik.icure.entities.Document

@Service
@Profile("app")
class DocumentByDataOwnerPatientDateFilter(
	private val documentDAO: DocumentDAO,
	private val sessionInformationProvider: SessionInformationProvider
) : Filter<String, Document, DocumentByDataOwnerPatientDateFilter> {

	override fun resolve(
		filter: DocumentByDataOwnerPatientDateFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = flow {
		documentDAO.listDocumentIdsByDataOwnerPatientCreated(
			datastoreInformation = datastoreInformation,
			searchKeys = sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId),
			secretForeignKeys = filter.secretPatientKeys,
			startDate = filter.startDate?.toEpochMilli(),
			endDate = filter.endDate?.toEpochMilli(),
			descending = filter.descending ?: false
		).also { emitAll(it) }
	}

}