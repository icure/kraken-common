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
	 * Bulk version of [getCustomEntityMetadataStub].
	 *
	 * The ids that do not match any existing custom entity are ignored, and duplicate ids are returned only once:
	 * the returned flow may contain fewer elements than [ids].
	 *
	 * The stubs are emitted in the same order as the corresponding ids in [ids], but this holds only as long as couchdb
	 * returns the rows of a view queried by keys in the order the keys were provided. That is the actual behaviour of
	 * all the supported couchdb versions, but it is not part of the documented api: if couchdb ever stops respecting
	 * that order the implementation still emits each existing stub exactly once, in an unspecified order, and logs a
	 * warning. The implementation never buffers the full result in memory, therefore it cannot reorder the stubs
	 * itself; the callers for which the order is critical should match the emitted stubs by [CustomEntityBase.id]
	 * instead of relying on their position.
	 */
	fun getCustomEntitiesMetadataStubs(datastoreInformation: IDatastoreInformation, ids: List<String>): Flow<CustomEntityBase>
}