package org.taktik.icure.domain.filter.impl.document

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.document.DocumentByDataOwnerTagFilter
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.base.HasEncryptionMetadata

class DocumentByDataOwnerTagFilter(
	override val dataOwnerId: String,
	override val tagType: String,
	override val tagCode: String?,
	override val desc: String?,
) : AbstractFilter<Document>,
	DocumentByDataOwnerTagFilter {
	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(
		item: Document,
		searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean
	): Boolean =
		searchKeyMatcher(dataOwnerId, item) &&
			if (tagCode != null)
				item.tags.any { it.type == tagType && it.code == tagCode }
			else
				item.tags.any { it.type == tagType }
}

