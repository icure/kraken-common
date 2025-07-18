package org.taktik.icure.domain.filter.impl.document

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.document.DocumentByDataOwnerPatientDateFilter
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.base.HasEncryptionMetadata
import java.time.Instant

data class DocumentByDataOwnerPatientDateFilter(
	override val dataOwnerId: String,
	override val secretPatientKeys: Set<String>,
	override val startDate: Instant? = null,
	override val endDate: Instant? = null,
	override val descending: Boolean? = null,
	override val desc: String? = null,
) : AbstractFilter<Document>,
	DocumentByDataOwnerPatientDateFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: Document, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = searchKeyMatcher(dataOwnerId, item) &&
		item.secretForeignKeys.intersect(secretPatientKeys).isNotEmpty() &&
		(startDate == null || (item.created != null && item.created >= startDate.toEpochMilli())) &&
		(endDate == null || (item.created != null && item.created <= endDate.toEpochMilli()))
}
