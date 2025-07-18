/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.EntityReference

interface EntityReferenceDAO : GenericDAO<EntityReference> {
	suspend fun getLatest(datastoreInformation: IDatastoreInformation, prefix: String): EntityReference?
}
