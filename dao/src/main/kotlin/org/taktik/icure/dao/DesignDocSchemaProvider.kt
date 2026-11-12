package org.taktik.icure.dao

import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.designdoc.DesignDocSchema

interface DesignDocSchemaProvider {
	suspend fun getOrRequestSchema(datastore: IDatastoreInformation): DesignDocSchema?
}