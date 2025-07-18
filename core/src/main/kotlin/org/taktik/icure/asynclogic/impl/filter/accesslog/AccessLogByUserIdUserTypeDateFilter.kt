package org.taktik.icure.asynclogic.impl.filter.accesslog

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.AccessLogDAO
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.accesslog.AccessLogByUserIdUserTypeDateFilter
import org.taktik.icure.entities.AccessLog

@Service
@Profile("app")
data class AccessLogByUserIdUserTypeDateFilter(
	private val accessLogDAO: AccessLogDAO
) : Filter<String, AccessLog, AccessLogByUserIdUserTypeDateFilter> {

	override fun resolve(
		filter: AccessLogByUserIdUserTypeDateFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = accessLogDAO.listAccessLogIdsByUserAfterDate(
		datastoreInformation = datastoreInformation,
		userId = filter.userId,
		accessType = filter.accessType,
		startDate = filter.startDate?.toEpochMilli(),
		descending = filter.descending ?: false
	)

}
