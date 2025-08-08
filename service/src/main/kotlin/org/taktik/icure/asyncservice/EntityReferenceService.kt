/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.entities.EntityReference

interface EntityReferenceService {
	suspend fun getLatest(prefix: String): EntityReference?

	/**
	 * Creates [EntityReference]s in batch
	 *
	 * @param entities a [Collection] of [EntityReference]s to create.
	 * @return a [Flow] containing all the [EntityReference]s that were successfully created.
	 */
	fun createEntityReferences(entities: Collection<EntityReference>): Flow<EntityReference>
}
