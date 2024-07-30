package org.taktik.icure.asynclogic.impl.filter.accesslog

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.AccessLogDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.accesslog.AccessLogByDateFilter
import org.taktik.icure.entities.AccessLog
import org.taktik.icure.utils.sortTimeBounds

@Service
@Profile("app")
class AccessLogByDateFilter(
	private val accessLogDAO: AccessLogDAO
) : Filter<String, AccessLog, AccessLogByDateFilter> {
	override fun resolve(
		filter: AccessLogByDateFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> {
		val (fromEpoch, toEpoch) = sortTimeBounds(filter.startDate, filter.endDate, filter.descending)
		return accessLogDAO.listAccessLogIdsByDate(
			datastoreInformation = datastoreInformation,
			fromEpoch = fromEpoch,
			toEpoch = toEpoch,
			descending = filter.descending ?: false
		)
	}
}