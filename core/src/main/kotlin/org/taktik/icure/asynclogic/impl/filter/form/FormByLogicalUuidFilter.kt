package org.taktik.icure.asynclogic.impl.filter.form

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.FormDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.form.FormByLogicalUuidFilter
import org.taktik.icure.entities.Form

@Service
@Profile("app")
class FormByLogicalUuidFilter(
	private val formDAO: FormDAO
) : Filter<String, Form, FormByLogicalUuidFilter> {

	override fun resolve(
		filter: FormByLogicalUuidFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = formDAO.listFormIdsByLogicalUuid(
		datastoreInformation = datastoreInformation,
		formUuid = filter.logicalUuid,
		descending = filter.descending ?: false
	)

}