package org.taktik.icure.asynclogic.impl.filter.document

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.DocumentDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.document.DocumentByTypeDataOwnerPatientFilter
import org.taktik.icure.entities.Document

@Service
@Profile("app")
class DocumentByTypeDataOwnerPatientFilter(
	private val documentDAO: DocumentDAO,
	private val sessionInformationProvider: SessionInformationProvider,
) : Filter<String, Document, DocumentByTypeDataOwnerPatientFilter> {
	override fun resolve(
		filter: DocumentByTypeDataOwnerPatientFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = flow {
		documentDAO
			.listDocumentIdsByDocumentTypeHcPartySecretMessageKeys(
				datastoreInformation = datastoreInformation,
				searchKeys = sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId),
				secretForeignKeys = filter.secretPatientKeys.toList(),
				documentTypeCode = filter.documentType.name,
			).also { emitAll(it) }
	}
}
