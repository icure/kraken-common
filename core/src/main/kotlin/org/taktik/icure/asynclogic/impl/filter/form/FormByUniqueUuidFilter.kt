package org.taktik.icure.asynclogic.impl.filter.form

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.FormDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.form.FormByUniqueUuidFilter
import org.taktik.icure.entities.Form

@Service
@Profile("app")
class FormByUniqueUuidFilter(
	private val formDAO: FormDAO,
) : Filter<String, Form, FormByUniqueUuidFilter> {
	override fun resolve(
		filter: FormByUniqueUuidFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = formDAO.listFormIdsByUniqueId(
		datastoreInformation = datastoreInformation,
		externalUuid = filter.uniqueId,
		descending = filter.descending ?: false,
	)
}
