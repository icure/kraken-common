package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.datastore.IDatastoreInformation

interface ConflictDAO<T: Identifiable<String>> : GenericDAO<T> {
	fun listConflicts(datastoreInformation: IDatastoreInformation): Flow<T>
}