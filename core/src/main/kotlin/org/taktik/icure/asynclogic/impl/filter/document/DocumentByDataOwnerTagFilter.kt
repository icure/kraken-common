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
import org.taktik.icure.domain.filter.document.DocumentByDataOwnerPatientDateFilter
import org.taktik.icure.domain.filter.document.DocumentByDataOwnerTagFilter
import org.taktik.icure.entities.Document

@Service
@Profile("app")
class DocumentByDataOwnerTagFilter(
	private val documentDAO: DocumentDAO,
	private val sessionInformationProvider: SessionInformationProvider,
) : Filter<String, Document, DocumentByDataOwnerTagFilter> {
	override fun resolve(
		filter: DocumentByDataOwnerTagFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = flow {
		documentDAO.listDocumentIdsByDataOwnerTags(
			datastoreInformation = datastoreInformation,
			searchKeys = sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId),
			tagCode = filter.tagCode,
			tagType = filter.tagType,
		).also { emitAll(it) }
	}
}
