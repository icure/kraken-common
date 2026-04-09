/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.service

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.service.ServiceByHcPartyCodesFilter
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.embed.Service

data class ServiceByHcPartyCodesFilter(
	override val desc: String? = null,
	override val healthcarePartyId: String,
	override val codeCodes: Map<String, Collection<String>>,
	override val startValueDate: Long? = null,
	override val endValueDate: Long? = null,
) : AbstractFilter<Service>,
	ServiceByHcPartyCodesFilter {

	override val canBeUsedInWebsocket = false
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(healthcarePartyId)

	override fun matches(item: Service, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		throw UnsupportedOperationException()
}
