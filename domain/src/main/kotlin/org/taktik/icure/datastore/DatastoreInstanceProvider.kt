package org.taktik.icure.datastore

interface DatastoreInstanceProvider {
	suspend fun getInstanceAndGroup(): IDatastoreInformation
}
