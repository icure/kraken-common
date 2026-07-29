package org.taktik.icure.asynclogic.impl.customentities

import org.taktik.icure.entities.CustomEntityBase

interface CustomEntityDefinitionLogicContext {
	suspend fun validateAndMapForCreation(entity: CustomEntityBase): CustomEntityBase
	suspend fun checkValidModification(currentEntityStub: CustomEntityBase, updatedEntity: CustomEntityBase)
	suspend fun filterValidModifications(currentEntitiesStubs: Collection<CustomEntityBase>, updatedEntities: Collection<CustomEntityBase>): Collection<CustomEntityBase>

	/**
	 * Called after a [CustomEntityBase] has been permanently purged, to clean up any resources associated to it that
	 * are not automatically removed together with the couchdb document itself (e.g. object-storage-backed attachments).
	 */
	suspend fun cleanupPurgedEntity(purgedEntity: CustomEntityBase) {}
}