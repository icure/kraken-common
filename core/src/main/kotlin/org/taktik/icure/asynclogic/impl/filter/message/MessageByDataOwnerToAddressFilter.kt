package org.taktik.icure.asynclogic.impl.filter.message

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.MessageDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.message.MessageByDataOwnerToAddressFilter
import org.taktik.icure.entities.Message
import org.taktik.icure.utils.mergeUniqueIdsForSearchKeys

@Service
@Profile("app")
class MessageByDataOwnerToAddressFilter(
	private val messageDAO: MessageDAO,
	private val sessionInformationProvider: SessionInformationProvider
) : Filter<String, Message, MessageByDataOwnerToAddressFilter> {

	override fun resolve(
		filter: MessageByDataOwnerToAddressFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = flow {
		emitAll(
			mergeUniqueIdsForSearchKeys(sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId)) { key ->
				messageDAO.listMessageIdsByToAddress(
					datastoreInformation = datastoreInformation,
					dataOwnerId = key,
					toAddress = filter.toAddress
				)
			}
		)
	}

}