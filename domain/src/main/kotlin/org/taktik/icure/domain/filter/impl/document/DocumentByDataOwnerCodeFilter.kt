package org.taktik.icure.domain.filter.impl.document

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.document.DocumentByDataOwnerCodeFilter
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.base.HasEncryptionMetadata

class DocumentByDataOwnerCodeFilter(
	override val dataOwnerId: String,
	override val codeType: String,
	override val codeCode: String?,
	override val desc: String? = null,
) : AbstractFilter<Document>,
	DocumentByDataOwnerCodeFilter {
	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(
		item: Document,
		searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean
	): Boolean =
		searchKeyMatcher(dataOwnerId, item) &&
			if (codeCode != null)
				item.codes.any { it.type == codeType && it.code == codeCode }
			else
				item.codes.any { it.type == codeType }
}
