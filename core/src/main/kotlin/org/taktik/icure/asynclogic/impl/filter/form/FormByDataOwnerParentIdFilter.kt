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
import org.taktik.icure.domain.filter.form.FormByDataOwnerParentIdFilter
import org.taktik.icure.entities.Form

@Service
@Profile("app")
class FormByDataOwnerParentIdFilter(
	private val formDAO: FormDAO,
	private val sessionInformationProvider: SessionInformationProvider
) : Filter<String, Form, FormByDataOwnerParentIdFilter> {

	override fun resolve(
		filter: FormByDataOwnerParentIdFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = flow {
		formDAO.listFormIdsByDataOwnerAndParentId(
			datastoreInformation = datastoreInformation,
			searchKeys = sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId),
			formId = filter.parentId
		).also { emitAll(it) }
	}
}
