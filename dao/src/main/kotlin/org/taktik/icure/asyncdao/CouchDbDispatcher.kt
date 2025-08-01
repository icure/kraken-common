package org.taktik.icure.asyncdao

import org.taktik.couchdb.Client
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation

interface CouchDbDispatcher {
	suspend fun getClient(datastoreInformation: IDatastoreInformation, retry: Int = 5): Client
}
