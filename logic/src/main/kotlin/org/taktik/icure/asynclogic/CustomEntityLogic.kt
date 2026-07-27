package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.entities.CustomEntityBase

interface CustomEntityLogic {
	suspend fun createCustomEntity(entityType: String, entity: CustomEntityBase): CustomEntityBase
	suspend fun getCustomEntity(entityType: String, id: String): CustomEntityBase?
	fun getCustomEntities(entityType: String, ids: List<String>): Flow<CustomEntityBase>
	suspend fun modifyCustomEntity(entityType: String, entity: CustomEntityBase): CustomEntityBase
	fun modifyCustomEntities(entityType: String, entities: List<CustomEntityBase>): Flow<CustomEntityBase>
}