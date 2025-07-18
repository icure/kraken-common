package org.taktik.icure.asynclogic.impl.filter.service

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.service.ServiceByAssociationIdFilter
import org.taktik.icure.entities.embed.Service

@org.springframework.stereotype.Service
@Profile("app")
class ServiceByAssociationIdFilter(
	private val contactDAO: ContactDAO
) : Filter<String, Service, ServiceByAssociationIdFilter> {
	override fun resolve(
		filter: ServiceByAssociationIdFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = contactDAO.listServiceIdsByAssociationId(
		datastoreInformation = datastoreInformation,
		associationId = filter.associationId
	)
}
