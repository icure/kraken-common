package org.taktik.icure.domain.filter.impl.code

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.code.CodeByQualifiedLinkFilter
import org.taktik.icure.entities.base.Code
import org.taktik.icure.entities.base.HasEncryptionMetadata

class CodeByQualifiedLinkFilter(
	override val linkType: String,
	override val linkedId: String?,
	override val desc: String?,
) : AbstractFilter<Code>,
	CodeByQualifiedLinkFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Code, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = item.qualifiedLinks.containsKey(linkType) &&
		(linkedId == null || item.qualifiedLinks.getValue(linkType).contains(linkType))
}
