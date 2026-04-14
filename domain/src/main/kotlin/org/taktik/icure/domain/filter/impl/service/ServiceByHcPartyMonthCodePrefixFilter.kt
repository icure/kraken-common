/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.service

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.service.ServiceByHcPartyMonthCodePrefixFilter
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.entities.embed.withEncryptionMetadata
import org.taktik.icure.utils.FuzzyDates

data class ServiceByHcPartyMonthCodePrefixFilter(
	override val desc: String? = null,
	override val healthcarePartyId: String,
	override val year: Int? = null,
	override val month: Int? = null,
	override val codeType: String,
	override val codeCodePrefix: String,
	override val startValueDate: Long? = null,
	override val endValueDate: Long? = null
) : AbstractFilter<Service>,
	ServiceByHcPartyMonthCodePrefixFilter {

	override val canBeUsedInWebsocket = false
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(healthcarePartyId)

	override fun matches(item: Service, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		if (item.endOfLife != null) return false
		if (item.withEncryptionMetadata()?.let { searchKeyMatcher(healthcarePartyId, it) } != true) return false
		if (!item.codes.any { codeType == it.type && it.code?.startsWith(codeCodePrefix) == true }) return false

		val date = item.valueDate ?: item.openingDate
		val dateAsInt = date?.let { if (it > FuzzyDates.MAX_FUZZY_DATE) (it / 1000000).toInt() else it.toInt() }
		val itemYear = dateAsInt?.let { it / 10000 }?.takeIf { it in 1000..9999 }
		val itemMonth = dateAsInt?.let { it / 100 % 100 }

		if (year == null && month == null) {
			if (itemYear != null) return false
		} else {
			if (itemYear != year) return false
			if (itemMonth != month) return false
		}

		if (startValueDate != null && (date == null || !FuzzyDates.isFuzzyDateAfterOrEqual(date, startValueDate))) return false
		if (endValueDate != null && (date == null || !FuzzyDates.isFuzzyDateBeforeOrEqual(date, endValueDate))) return false
		return true
	}
}
