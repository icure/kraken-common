/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.service

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.service.ServiceByHcPartyPatientTagPrefixFilter
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.entities.embed.withEncryptionMetadata

data class ServiceByHcPartyPatientTagPrefixFilter(
	override val desc: String? = null,
	override val healthcarePartyId: String,
	override val patientSecretForeignKeys: Set<String>,
	override val tagType: String,
	override val tagCodePrefix: String,
	override val startValueDate: Long? = null,
	override val endValueDate: Long? = null

) : AbstractFilter<Service>,
	ServiceByHcPartyPatientTagPrefixFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(healthcarePartyId)

	override fun matches(item: Service, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = (
		item.endOfLife == null &&
			(item.withEncryptionMetadata()?.let { searchKeyMatcher(healthcarePartyId, it) } == true) &&
			(item.secretForeignKeys?.intersect(patientSecretForeignKeys.toSet())?.isNotEmpty() == true) &&
			(item.tags.any { tagType == it.type && it.code?.startsWith(tagCodePrefix) == true }) &&
			(startValueDate == null || (item.valueDate ?: item.openingDate)?.let { it >= startValueDate } == true) &&
			(endValueDate == null || (item.valueDate ?: item.openingDate)?.let { it <= endValueDate } == true)
		)
}
