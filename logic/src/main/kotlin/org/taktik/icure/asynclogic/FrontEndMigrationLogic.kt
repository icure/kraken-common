/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.entities.FrontEndMigration

interface FrontEndMigrationLogic : EntityPersister<FrontEndMigration> {
	suspend fun createFrontEndMigration(frontEndMigration: FrontEndMigration): FrontEndMigration?

	suspend fun getFrontEndMigration(frontEndMigrationId: String): FrontEndMigration?
	fun getFrontEndMigrationByUserIdName(userId: String, name: String?): Flow<FrontEndMigration>

	suspend fun modifyFrontEndMigration(frontEndMigration: FrontEndMigration): FrontEndMigration?
}
