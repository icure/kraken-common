package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.asyncdao.results.BulkSaveResult
import org.taktik.icure.datastore.IDatastoreInformation

interface ConflictDAO<T: Identifiable<String>> : GenericDAO<T> {
	fun listConflicts(datastoreInformation: IDatastoreInformation): Flow<T>
	fun listIdsOfEntitiesWithConflicts(datastoreInformation: IDatastoreInformation): Flow<String>
	fun purgeConflictingRevisions(
		datastoreInformation: IDatastoreInformation,
		entityId: String,
		revisionsToPurge: List<String>
	): Flow<BulkSaveResult<DocIdentifier>>
}