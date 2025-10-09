/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.service

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.service.ServiceByHcPartyCodePrefixFilter
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.embed.Service

data class ServiceByHcPartyCodePrefixFilter(
	override val desc: String? = null,
	override val healthcarePartyId: String,
	override val codeType: String,
	override val codeCodePrefix: String,
) : AbstractFilter<Service>,
	ServiceByHcPartyCodePrefixFilter {

	override val canBeUsedInWebsocket = false
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(healthcarePartyId)

	override fun matches(item: Service, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		throw UnsupportedOperationException()
}
