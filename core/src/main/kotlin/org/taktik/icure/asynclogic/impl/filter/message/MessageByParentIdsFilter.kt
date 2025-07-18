package org.taktik.icure.asynclogic.impl.filter.message

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.MessageDAO
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.domain.filter.message.MessageByParentIdsFilter
import org.taktik.icure.entities.Message

@Service
@Profile("app")
class MessageByParentIdsFilter(
	private val messageDAO: MessageDAO
) : Filter<String, Message, MessageByParentIdsFilter> {
	override fun resolve(
		filter: MessageByParentIdsFilter,
		context: org.taktik.icure.asynclogic.impl.filter.Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = messageDAO.listMessageIdsByParents(
		datastoreInformation = datastoreInformation,
		parentIds = filter.parentIds
	)

}
