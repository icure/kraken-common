package org.taktik.icure.asyncdao

import org.taktik.icure.entities.designdoc.DesignDocSchema

interface DesignDocSchemaDAO {
	suspend fun get(id: String): DesignDocSchema?
	suspend fun create(designDocSchema: DesignDocSchema): DesignDocSchema
}