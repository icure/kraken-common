package org.taktik.icure.domain.filter.impl.service

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.domain.filter.service.ServiceByQualifiedLinkFilter
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.LinkQualification

data class ServiceByQualifiedLinkFilter(
	override val linkValues: List<String>,
	override val linkQualification: LinkQualification? = null,
	override val desc: String? = null
) : AbstractFilter<Service>, ServiceByQualifiedLinkFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Service, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		if(linkQualification != null) {
			item.qualifiedLinks[linkQualification]?.values?.any {
				linkValues.contains(it)
			} == true
		} else item.qualifiedLinks.values.any { links ->
			links.values.any { linkValues.contains(it) }
		}

}