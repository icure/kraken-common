package org.taktik.icure.domain.filter.impl.form

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.form.FormByDataOwnerPatientOpeningDateFilter
import org.taktik.icure.entities.Form
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class FormByDataOwnerPatientOpeningDateFilter(
	override val dataOwnerId: String,
	override val secretPatientKeys: Set<String>,
	override val startDate: Long? = null,
	override val endDate: Long? = null,
	override val descending: Boolean? = null,
	override val desc: String? = null
) : AbstractFilter<Form>, FormByDataOwnerPatientOpeningDateFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: Form, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		searchKeyMatcher(dataOwnerId, item)
			&& item.secretForeignKeys.intersect(secretPatientKeys).isNotEmpty()
			&& (startDate == null || (item.openingDate != null && item.openingDate >= startDate))
			&& (endDate == null || (item.openingDate != null && item.openingDate <= endDate))

}
