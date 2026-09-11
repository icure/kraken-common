package org.taktik.icure.asynclogic.base

import org.taktik.icure.datastore.IDatastoreInformation

interface ProxyDatastoreProvider {
	suspend fun getInstanceAndGroup(): IDatastoreInformation
}