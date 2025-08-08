package org.taktik.icure.domain.filter.impl.document

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.document.DocumentByTypeDataOwnerPatientFilter
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.embed.DocumentType

data class DocumentByTypeDataOwnerPatientFilter(
	override val dataOwnerId: String,
	override val documentType: DocumentType,
	override val secretPatientKeys: Set<String>,
	override val desc: String? = null,
) : AbstractFilter<Document>,
	DocumentByTypeDataOwnerPatientFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: Document, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = searchKeyMatcher(dataOwnerId, item) &&
		item.secretForeignKeys.intersect(secretPatientKeys).isNotEmpty() &&
		item.documentType == documentType
}
