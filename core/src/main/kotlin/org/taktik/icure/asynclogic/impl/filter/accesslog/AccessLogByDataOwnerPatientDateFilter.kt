package org.taktik.icure.asynclogic.impl.filter.accesslog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.AccessLogDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.accesslog.AccessLogByDataOwnerPatientDateFilter
import org.taktik.icure.entities.AccessLog

@Service
@Profile("app")
data class AccessLogByDataOwnerPatientDateFilter(
	private val accessLogDAO: AccessLogDAO,
	private val sessionInformationProvider: SessionInformationProvider,
) : Filter<String, AccessLog, AccessLogByDataOwnerPatientDateFilter> {
	override fun resolve(
		filter: AccessLogByDataOwnerPatientDateFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = flow {
		accessLogDAO
			.listAccessLogIdsByDataOwnerPatientDate(
				datastoreInformation = datastoreInformation,
				searchKeys = sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId),
				secretForeignKeys = filter.secretPatientIds,
				startDate = filter.startDate?.toEpochMilli(),
				endDate = filter.endDate?.toEpochMilli(),
				descending = filter.descending ?: false,
			).also {
				emitAll(it)
			}
	}
}
