package org.taktik.icure.domain.filter.impl.form

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.form.FormByUniqueUuidFilter
import org.taktik.icure.entities.Form
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class FormByUniqueUuidFilter(
	override val uniqueId: String,
	override val descending: Boolean? = null,
	override val desc: String? = null
) : AbstractFilter<Form>, FormByUniqueUuidFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Form, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		item.uniqueId == uniqueId
}
