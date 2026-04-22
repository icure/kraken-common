package org.taktik.icure.asyncdao

import org.taktik.icure.entities.designdoc.DesignDocEntityConfiguration

interface DesignDocDAO {
	suspend fun getLatestConfigForEntity(applicationId: String, entityType: String): DesignDocEntityConfiguration?
}