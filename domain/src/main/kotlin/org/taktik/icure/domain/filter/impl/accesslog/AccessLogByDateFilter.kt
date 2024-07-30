package org.taktik.icure.domain.filter.impl.accesslog

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.AccessLog
import org.taktik.icure.entities.base.HasEncryptionMetadata
import java.time.Instant
import org.taktik.icure.domain.filter.accesslog.AccessLogByDateFilter as IAccessLogByDateFilter

data class AccessLogByDateFilter(
	override val startDate: Instant? = null,
	override val endDate: Instant? = null,
	override val descending: Boolean? = null,
	override val desc: String? = null
) : AbstractFilter<AccessLog>, IAccessLogByDateFilter {

	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: AccessLog, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		item.date != null && (startDate == null || item.date >= startDate) && (endDate == null || item.date <= endDate)


}
