package org.taktik.icure.domain.filter.impl.form

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.form.FormByLogicalUuidFilter
import org.taktik.icure.entities.Form
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class FormByLogicalUuidFilter(
	override val logicalUuid: String,
	override val descending: Boolean? = null,
	override val desc: String? = null,
) : AbstractFilter<Form>,
	FormByLogicalUuidFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Form, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = item.logicalUuid == logicalUuid
}
