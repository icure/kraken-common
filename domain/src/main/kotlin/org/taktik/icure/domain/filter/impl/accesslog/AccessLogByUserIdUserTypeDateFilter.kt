package org.taktik.icure.domain.filter.impl.accesslog

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.AccessLog
import org.taktik.icure.entities.base.HasEncryptionMetadata
import java.time.Instant
import org.taktik.icure.domain.filter.accesslog.AccessLogByUserIdUserTypeDateFilter as IAccessLogByUserIdUserTypeDateFilter

data class AccessLogByUserIdUserTypeDateFilter(
	override val userId: String,
	override val accessType: String?,
	override val startDate: Instant?,
	override val descending: Boolean?,
	override val desc: String? = null,
) : AbstractFilter<AccessLog>,
	IAccessLogByUserIdUserTypeDateFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: AccessLog, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = item.user == userId &&
		(accessType == null || accessType == item.accessType) &&
		item.date != null &&
		(
			(descending != true && (startDate == null || item.date >= startDate)) ||
				(descending == true && (startDate == null || item.date <= startDate))
			)
}
