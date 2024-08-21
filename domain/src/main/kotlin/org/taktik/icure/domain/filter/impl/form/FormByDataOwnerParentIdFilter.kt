package org.taktik.icure.domain.filter.impl.form

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.form.FormByDataOwnerParentIdFilter
import org.taktik.icure.entities.Form
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class FormByDataOwnerParentIdFilter(
	override val dataOwnerId: String,
	override val parentId: String,
	override val desc: String? = null
) : AbstractFilter<Form>, FormByDataOwnerParentIdFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: Form, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		searchKeyMatcher(dataOwnerId, item) && item.parent == parentId
}
