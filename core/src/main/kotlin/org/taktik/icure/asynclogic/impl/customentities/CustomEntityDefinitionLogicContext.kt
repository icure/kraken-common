package org.taktik.icure.asynclogic.impl.customentities

import com.icure.cardinal.customentities.config.CustomEntityDefinition
import kotlinx.coroutines.flow.Flow
import org.taktik.icure.asyncdao.CustomEntityDAO
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.CustomEntityBase

interface CustomEntityDefinitionLogicContext {
	// Currently no special validation on create (no autofix)
	suspend fun checkValidModification(currentEntityStub: CustomEntityBase, updatedEntity: CustomEntityBase)
	suspend fun filterValidModifications(currentEntitiesStubs: Collection<CustomEntityBase>, updatedEntities: Collection<CustomEntityBase>): Collection<CustomEntityBase>
}