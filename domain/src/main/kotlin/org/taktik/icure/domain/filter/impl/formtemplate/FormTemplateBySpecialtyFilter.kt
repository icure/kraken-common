package org.taktik.icure.domain.filter.impl.formtemplate

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.formtemplate.FormTemplateBySpecialtyFilter
import org.taktik.icure.entities.FormTemplate
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class FormTemplateBySpecialtyFilter(
	override val specialtyCode: String,
	override val desc: String? = null,
) : AbstractFilter<FormTemplate>,
	FormTemplateBySpecialtyFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: FormTemplate, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		item.specialty?.code == specialtyCode
}