package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.CustomEntityBase

interface CustomEntityDAO : GenericDAO<CustomEntityBase>, AttachmentManagementDAO<CustomEntityBase> {
	/**
	 * Get a custom entity by id, but without the actual content ([CustomEntityBase.extensions] is always null)
	 */
	suspend fun getCustomEntityMetadataStub(datastoreInformation: IDatastoreInformation, id: String): CustomEntityBase?

	/**
	 * Bulk version of [getCustomEntityMetadataStub]
	 */
	fun getCustomEntitiesMetadataStubs(datastoreInformation: IDatastoreInformation, ids: List<String>): Flow<CustomEntityBase>
}