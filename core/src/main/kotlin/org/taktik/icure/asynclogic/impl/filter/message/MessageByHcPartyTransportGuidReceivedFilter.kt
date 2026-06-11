package org.taktik.icure.asynclogic.impl.filter.message

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.MessageDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.ConfigurationView
import org.taktik.icure.domain.filter.message.MessageByHcPartyTransportGuidReceivedFilter
import org.taktik.icure.entities.Message

@Service
@Profile("app")
class MessageByHcPartyTransportGuidReceivedFilter(
	private val messageDAO: MessageDAO,
) : Filter<String, Message, MessageByHcPartyTransportGuidReceivedFilter> {
	override val configurationViews = listOf(ConfigurationView("Message", "by_all_delegates_transport_guid_received"))

	override fun resolve(
		filter: MessageByHcPartyTransportGuidReceivedFilter,
		context: org.taktik.icure.asynclogic.impl.filter.Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = messageDAO.listMessageIdsByTransportGuidReceived(
		datastoreInformation = datastoreInformation,
		dataOwnerId = filter.healthcarePartyId,
		transportGuid = filter.transportGuid,
		descending = filter.descending ?: false,
	)
}
