/*
 * Copyright (c) 2026. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.service

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.VersionFiltering
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.entities.embed.withEncryptionMetadata
import org.taktik.icure.utils.FuzzyDates

data class ServiceByDataOwnerCodeCodesFilter(
	override val desc: String? = null,
	override val dataOwnerId: String,
	override val codeCodes: Map<String, Collection<String>>,
	override val startValueDate: Long? = null,
	override val endValueDate: Long? = null,
	override val filterVersion: VersionFiltering? = null,
) : AbstractFilter<Service>,
	org.taktik.icure.domain.filter.service.ServiceByDataOwnerCodeCodesFilter {

	override val canBeUsedInWebsocket = false
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	// Cannot verify in memory whether the item is the latest version of the service: checks only the other criteria.
	override fun matches(item: Service, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = (
		item.endOfLife == null &&
			(item.withEncryptionMetadata()?.let { searchKeyMatcher(dataOwnerId, it) } == true) &&
			(item.codes.any { codeCodes[it.type]?.let { codes -> it.code in codes } == true }) &&
			(startValueDate == null || (item.valueDate ?: item.openingDate)?.let { FuzzyDates.isFuzzyDateAfterOrEqual(it, startValueDate) } == true) &&
			(endValueDate == null || (item.valueDate ?: item.openingDate)?.let { FuzzyDates.isFuzzyDateBeforeOrEqual(it, endValueDate) } == true)
		)
}
