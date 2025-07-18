package org.taktik.icure.asynclogic.impl.filter.message

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.MessageDAO
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.message.MessageByDataOwnerTransportGuidSentDateFilter
import org.taktik.icure.entities.Message

@Service
@Profile("app")
class MessageByDataOwnerTransportGuidSentDateFilter(
	private val messageDAO: MessageDAO
) : Filter<String, Message, MessageByDataOwnerTransportGuidSentDateFilter> {

	override fun resolve(
		filter: MessageByDataOwnerTransportGuidSentDateFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = messageDAO.listMessageIdsByTransportGuidAndSentDate(
		datastoreInformation = datastoreInformation,
		dataOwnerId = filter.dataOwnerId,
		transportGuid = filter.transportGuid,
		fromDate = filter.fromDate?.toEpochMilli(),
		toDate = filter.toDate?.toEpochMilli(),
		descending = filter.descending ?: false
	)

}
