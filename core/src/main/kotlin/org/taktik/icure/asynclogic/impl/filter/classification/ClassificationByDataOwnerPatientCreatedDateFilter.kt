package org.taktik.icure.asynclogic.impl.filter.classification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.ClassificationDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.classification.ClassificationByDataOwnerPatientCreatedDateFilter
import org.taktik.icure.entities.Classification

@Service
@Profile("app")
class ClassificationByDataOwnerPatientCreatedDateFilter(
	val classificationDAO: ClassificationDAO,
	val sessionInformationProvider: SessionInformationProvider
) : Filter<String, Classification, ClassificationByDataOwnerPatientCreatedDateFilter> {

	override fun resolve(
		filter: ClassificationByDataOwnerPatientCreatedDateFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = flow {
		classificationDAO.listClassificationIdsByDataOwnerPatientCreated(
			datastoreInformation = datastoreInformation,
			searchKeys = sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId),
			secretForeignKeys = filter.secretForeignKeys,
			startDate = filter.startDate,
			endDate = filter.endDate,
			descending = filter.descending ?: false
		).also { emitAll(it) }
	}

}
