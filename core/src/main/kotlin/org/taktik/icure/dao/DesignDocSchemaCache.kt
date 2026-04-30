package org.taktik.icure.dao

import org.taktik.icure.entities.designdoc.DesignDocSchema

interface DesignDocSchemaCache {
	suspend fun getOrRequestSchema(id: String): DesignDocSchema?
}