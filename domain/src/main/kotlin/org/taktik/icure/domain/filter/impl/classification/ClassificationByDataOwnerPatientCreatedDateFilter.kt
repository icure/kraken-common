package org.taktik.icure.domain.filter.impl.classification

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.classification.ClassificationByDataOwnerPatientCreatedDateFilter
import org.taktik.icure.entities.Classification
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class ClassificationByDataOwnerPatientCreatedDateFilter(
	override val dataOwnerId: String,
	override val startDate: Long?,
	override val endDate: Long?,
	override val secretForeignKeys: Set<String>,
	override val descending: Boolean?,
	override val desc: String? = null,
) : AbstractFilter<Classification>,
	ClassificationByDataOwnerPatientCreatedDateFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: Classification, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = searchKeyMatcher(dataOwnerId, item) &&
		item.secretForeignKeys.intersect(secretForeignKeys).isNotEmpty() &&
		item.created != null &&
		(startDate == null || item.created >= startDate) &&
		(endDate == null || item.created <= endDate)
}
