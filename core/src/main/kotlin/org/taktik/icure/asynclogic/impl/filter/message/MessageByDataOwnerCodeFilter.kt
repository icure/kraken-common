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
import org.taktik.icure.domain.filter.message.MessageByDataOwnerCodeFilter
import org.taktik.icure.entities.Message

@Service
@Profile("app")
class MessageByDataOwnerCodeFilter(
	private val documentDAO: MessageDAO,
	private val sessionInformationProvider: SessionInformationProvider,
) : Filter<String, Message, MessageByDataOwnerCodeFilter> {
	override fun resolve(
		filter: MessageByDataOwnerCodeFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = flow {
		documentDAO.listMessageIdsByDataOwnerCode(
			datastoreInformation = datastoreInformation,
			searchKeys = sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId),
			codeCode = filter.codeCode,
			codeType = filter.codeType,
		).also { emitAll(it) }
	}
}
