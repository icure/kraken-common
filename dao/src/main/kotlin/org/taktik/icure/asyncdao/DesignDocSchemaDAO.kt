package org.taktik.icure.asyncdao

import org.taktik.icure.entities.designdoc.DesignDocSchema

interface DesignDocSchemaDAO {
	suspend fun get(applicationId: String, version: Int): DesignDocSchema?
}