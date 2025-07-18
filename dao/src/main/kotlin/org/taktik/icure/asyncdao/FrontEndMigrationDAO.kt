/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.FrontEndMigration

interface FrontEndMigrationDAO : GenericDAO<FrontEndMigration> {
	fun getFrontEndMigrationsByUserIdAndName(datastoreInformation: IDatastoreInformation, userId: String, name: String?): Flow<FrontEndMigration>
}
