package org.taktik.icure.domain.filter.formtemplate

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.FormTemplate

/**
 * Retrieves all the [FormTemplate]s where [FormTemplate.specialty] code is equal to [specialtyCode].
 * This filter requires a special permission to be used.
 */
interface FormTemplateBySpecialtyFilter : Filter<String, FormTemplate> {
	val specialtyCode: String
}