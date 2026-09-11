package org.taktik.icure.cache

import org.taktik.icure.entities.CustomEntityBase

interface CustomEntityCache : EntityCache<String, CustomEntityBase> {
	suspend fun putStub(id: String, entity: CustomEntityBase)
	suspend fun getStub(id: String): CustomEntityBase?
}