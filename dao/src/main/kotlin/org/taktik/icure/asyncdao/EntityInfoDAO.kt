package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.base.EntityInfo

interface EntityInfoDAO {
	fun getEntitiesInfo(datastoreInformation: IDatastoreInformation, ids: Collection<String>): Flow<EntityInfo>
}
