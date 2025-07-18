package org.taktik.icure.asynclogic.impl.filter.form

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.FormDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.form.FormByDataOwnerPatientOpeningDateFilter
import org.taktik.icure.entities.Form

@Service
@Profile("app")
class FormByDataOwnerPatientOpeningDateFilter(
	private val formDAO: FormDAO,
	private val sessionInformationProvider: SessionInformationProvider
) : Filter<String, Form, FormByDataOwnerPatientOpeningDateFilter> {
	override fun resolve(
		filter: FormByDataOwnerPatientOpeningDateFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = flow {
		formDAO.listFormIdsByDataOwnerPatientOpeningDate(
			datastoreInformation = datastoreInformation,
			searchKeys = sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId),
			secretForeignKeys = filter.secretPatientKeys,
			startDate = filter.startDate,
			endDate = filter.endDate,
			descending = filter.descending ?: false
		).also { emitAll(it) }
	}
}
