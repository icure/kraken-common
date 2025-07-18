package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.RecoveryData

interface RecoveryDataDAO : GenericDAO<RecoveryData> {
	/**
	 * Get the ids all recovery data for a certain recipient and optionally a certain type.
	 * If type is null, all the recovery data ids for the recipient are returned.
	 */
	fun findRecoveryDataIdsByRecipientAndType(
		datastoreInformation: IDatastoreInformation,
		recipient: String,
		type: RecoveryData.Type? = null,
	): Flow<RecoveryData>

	/**
	 * Get the ids of all recovery data that have expiration less than the provided value.
	 */
	fun findRecoveryDataIdsWithExpirationLessThan(
		datastoreInformation: IDatastoreInformation,
		expiration: Long,
	): Flow<RecoveryData>
}
