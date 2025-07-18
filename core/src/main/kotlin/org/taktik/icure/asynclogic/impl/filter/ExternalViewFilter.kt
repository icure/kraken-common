package org.taktik.icure.asynclogic.impl.filter

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.asyncdao.impl.GenericDAOImpl
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.ExternalViewFilter
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.embed.Service as ICureService

@Service
@Profile("app")
class ExternalViewFilter<O : Identifiable<String>>(
	private val daoList: List<GenericDAOImpl<*>>
) : Filter<String, O, ExternalViewFilter<O>>{

	override fun resolve(
		filter: ExternalViewFilter<O>,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> {
		// In case I have a service, I have to get the Contact DAO instead.
		val filterCanonicalName = filter.entityQualifiedName.takeIf {
			it != ICureService::class.java.canonicalName
		} ?: Contact::class.java.canonicalName
		return daoList.firstOrNull {
			it.entityClass.canonicalName == filterCanonicalName
		}?.listEntitiesIdInCustomView(
			datastoreInformation = datastoreInformation,
			viewName = filter.view,
			partitionName = filter.partition,
			startKey = filter.startKey,
			endKey = filter.endKey
		) ?: throw IllegalArgumentException("Logic for ${filter.entityQualifiedName} not found")
	}
}
