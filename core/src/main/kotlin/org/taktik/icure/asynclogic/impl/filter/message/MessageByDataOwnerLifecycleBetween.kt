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
import org.taktik.icure.domain.filter.message.MessageByDataOwnerLifecycleBetween
import org.taktik.icure.entities.Message
import org.taktik.icure.utils.mergeUniqueIdsForSearchKeys

@Service
@Profile("app")
class MessageByDataOwnerLifecycleBetween(
	private val messageDAO: MessageDAO,
	private val sessionInformationProvider: SessionInformationProvider
) : Filter<String, Message, MessageByDataOwnerLifecycleBetween> {

	override fun resolve(
		filter: MessageByDataOwnerLifecycleBetween,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = flow {
		mergeUniqueIdsForSearchKeys(sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId)) { key ->
			messageDAO.listMessageIdsByDataOwnerLifecycleBetween(
				datastoreInformation = datastoreInformation,
				searchKey = key,
				startTimestamp = filter.startTimestamp,
				endTimestamp = filter.endTimestamp,
				descending = filter.descending
			)
		}.let { emitAll(it) }
	}

}
