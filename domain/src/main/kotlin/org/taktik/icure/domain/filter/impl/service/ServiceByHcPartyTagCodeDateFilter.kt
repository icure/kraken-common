/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.service

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.entities.embed.withEncryptionMetadata

data class ServiceByHcPartyTagCodeDateFilter(
	override val desc: String? = null,
	override val healthcarePartyId: String? = null,
	override val patientSecretForeignKey: String? = null,
	override val tagType: String? = null,
	override val tagCode: String? = null,
	override val codeType: String? = null,
	override val codeCode: String? = null,
	override val startValueDate: Long? = null,
	override val endValueDate: Long? = null,
	override val descending: Boolean = false,
) : AbstractFilter<Service>, org.taktik.icure.domain.filter.service.ServiceByHcPartyTagCodeDateFilter {
	init {
		if (tagCode != null) {
			require(tagType != null) { "If you specify tagCode you must also specify tagType" }
		}
		if (codeCode != null) {
			require(codeType != null) { "If you specify codeCode you must also specify codeType" }
		}
	}

	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = healthcarePartyId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: Service, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		return (
			item.endOfLife == null &&
			(healthcarePartyId == null || item.withEncryptionMetadata()?.let { searchKeyMatcher(healthcarePartyId, it) } == true) &&
			(patientSecretForeignKey == null || item.secretForeignKeys != null && item.secretForeignKeys.contains(patientSecretForeignKey)) &&
			(tagType == null || item.tags.any { tagType == it.type && (tagCode == null || tagCode == it.code) }) &&
			(codeType == null || item.codes.any { codeType == it.type && (codeCode == null || codeCode == it.code) }) &&
			(startValueDate == null || item.valueDate != null && item.valueDate > startValueDate || item.openingDate != null && item.openingDate > startValueDate) &&
			(endValueDate == null || item.valueDate != null && item.valueDate < endValueDate || item.openingDate != null && item.openingDate < endValueDate)
		)
	}
}
