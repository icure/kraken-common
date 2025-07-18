package org.taktik.icure.asynclogic.impl.filter.message

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.MessageDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.message.MessageByDataOwnerPatientSentDateFilter
import org.taktik.icure.entities.Message

@Service
@Profile("app")
class MessageByDataOwnerPatientSentDateFilter(
	private val messageDAO: MessageDAO,
	private val sessionInformationProvider: SessionInformationProvider
) : Filter<String, Message, MessageByDataOwnerPatientSentDateFilter> {
	override fun resolve(
		filter: MessageByDataOwnerPatientSentDateFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = flow {
		messageDAO.listMessageIdsByDataOwnerPatientSentDate(
			datastoreInformation = datastoreInformation,
			searchKeys = sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId),
			secretForeignKeys = filter.secretPatientKeys,
			startDate = filter.startDate?.toEpochMilli(),
			endDate = filter.endDate?.toEpochMilli(),
			descending = filter.descending ?: false
		).also { emitAll(it) }
	}
}
