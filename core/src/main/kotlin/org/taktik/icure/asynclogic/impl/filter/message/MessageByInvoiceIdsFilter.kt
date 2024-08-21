package org.taktik.icure.asynclogic.impl.filter.message

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.MessageDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.domain.filter.message.MessageByInvoiceIdsFilter
import org.taktik.icure.entities.Message

@Service
@Profile("app")
class MessageByInvoiceIdsFilter(
	private val messageDAO: MessageDAO
) : Filter<String, Message, MessageByInvoiceIdsFilter> {
	override fun resolve(
		filter: MessageByInvoiceIdsFilter,
		context: org.taktik.icure.asynclogic.impl.filter.Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = messageDAO.listMessageIdsByInvoiceIds(
		datastoreInformation = datastoreInformation,
		invoiceIds = filter.invoiceIds
	)

}