package org.taktik.icure.domain.filter.impl.service

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.service.ServiceByDataOwnerPatientDateFilter
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.entities.embed.withEncryptionMetadata

data class ServiceByDataOwnerPatientDateFilter(
	override val dataOwnerId: String,
	override val secretForeignKeys: Set<String>,
	override val startDate: Long? = null,
	override val endDate: Long? = null,
	override val descending: Boolean? = null,
	override val desc: String? = null
) : AbstractFilter<Service>, ServiceByDataOwnerPatientDateFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: Service, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		(item.withEncryptionMetadata()?.let {
			searchKeyMatcher(dataOwnerId, it)
				&& it.secretForeignKeys.intersect(secretForeignKeys).isNotEmpty()
		} ?: false) && (item.valueDate ?: item.openingDate).let { date ->
			(date == null && startDate == null && endDate == null) ||
				date != null && (startDate == null || date >= startDate) && (endDate == null || date <= endDate)
		}

}