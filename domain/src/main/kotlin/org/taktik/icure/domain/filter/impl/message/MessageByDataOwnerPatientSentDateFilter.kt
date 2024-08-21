package org.taktik.icure.domain.filter.impl.message

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.message.MessageByDataOwnerPatientSentDateFilter
import org.taktik.icure.entities.Message
import org.taktik.icure.entities.base.HasEncryptionMetadata
import java.time.Instant

data class MessageByDataOwnerPatientSentDateFilter(
	override val dataOwnerId: String,
	override val secretPatientKeys: Set<String>,
	override val startDate: Instant? = null,
	override val endDate: Instant? = null,
	override val descending: Boolean?,
	override val desc: String? = null,
) : AbstractFilter<Message>,
	MessageByDataOwnerPatientSentDateFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: Message, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		searchKeyMatcher(dataOwnerId, item)
			&& item.secretForeignKeys.intersect(secretPatientKeys).isNotEmpty()
			&& ((item.sent == null && startDate == null && endDate == null) ||
				item.sent != null && (startDate == null || item.sent >= startDate.toEpochMilli()) && (endDate == null || item.sent <= endDate.toEpochMilli())
			)

}