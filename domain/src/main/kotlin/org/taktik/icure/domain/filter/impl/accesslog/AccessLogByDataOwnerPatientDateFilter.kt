package org.taktik.icure.domain.filter.impl.accesslog

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.AccessLog
import org.taktik.icure.entities.base.HasEncryptionMetadata
import java.time.Instant
import org.taktik.icure.domain.filter.accesslog.AccessLogByDataOwnerPatientDateFilter as IAccessLogByDataOwnerPatientDateFilter

data class AccessLogByDataOwnerPatientDateFilter(
	override val dataOwnerId: String,
	override val startDate: Instant?,
	override val endDate: Instant?,
	override val secretPatientIds: Set<String>,
	override val descending: Boolean?,
	override val desc: String? = null,
) : AbstractFilter<AccessLog>,
	IAccessLogByDataOwnerPatientDateFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: AccessLog, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = searchKeyMatcher(dataOwnerId, item) &&
		item.secretForeignKeys.intersect(secretPatientIds).isNotEmpty() &&
		(startDate == null || (item.date != null && item.date >= startDate)) &&
		(endDate == null || (item.date != null && item.date <= endDate))
}
