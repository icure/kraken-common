package org.taktik.icure.asynclogic.impl.filter.formtemplate

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.FormTemplateDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.formtemplate.FormTemplateBySpecialtyFilter
import org.taktik.icure.entities.FormTemplate

@Service
@Profile("app")
class FormTemplateBySpecialtyFilter(
	private val formTemplateDAO: FormTemplateDAO,
) : Filter<String, FormTemplate, FormTemplateBySpecialtyFilter> {
	override fun resolve(
		filter: FormTemplateBySpecialtyFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = formTemplateDAO.listFormTemplateIdsBySpecialty(
		datastoreInformation = datastoreInformation,
		specialtyCode = filter.specialtyCode,
	)
}