/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import org.taktik.icure.entities.EntityReference

interface EntityReferenceLogic : EntityPersister<EntityReference> {
	suspend fun getLatest(prefix: String): EntityReference?
}
