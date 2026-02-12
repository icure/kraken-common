package org.taktik.icure.asynclogic.impl.filter.message

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.MessageDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.message.MessageByDataOwnerTagFilter
import org.taktik.icure.entities.Message

@Service
@Profile("app")
class MessageByDataOwnerTagFilter(
	private val messageDAO: MessageDAO,
	private val sessionInformationProvider: SessionInformationProvider,
) : Filter<String, Message, MessageByDataOwnerTagFilter> {
	override fun resolve(
		filter: MessageByDataOwnerTagFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = flow {
		messageDAO.listMessageIdsByDataOwnerTag(
			datastoreInformation = datastoreInformation,
			searchKeys = sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId),
			tagCode = filter.tagCode,
			tagType = filter.tagType,
		).also { emitAll(it) }
	}
}
