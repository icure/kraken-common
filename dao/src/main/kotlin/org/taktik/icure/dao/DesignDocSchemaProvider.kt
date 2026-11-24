package org.taktik.icure.dao

import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.designdoc.DesignDocSchema
import org.taktik.icure.entities.designdoc.DesignDocSchemaStub

interface DesignDocSchemaProvider {
	suspend fun getOrRequestSchema(datastore: IDatastoreInformation): DesignDocSchemaStub?
}