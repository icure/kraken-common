/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.entities.FrontEndMigration

interface FrontEndMigrationService {
	suspend fun createFrontEndMigration(frontEndMigration: FrontEndMigration): FrontEndMigration
	suspend fun deleteFrontEndMigration(frontEndMigrationId: String): FrontEndMigration?

	suspend fun getFrontEndMigration(frontEndMigrationId: String): FrontEndMigration?
	fun getFrontEndMigrationByUserIdName(userId: String, name: String?): Flow<FrontEndMigration>

	suspend fun modifyFrontEndMigration(frontEndMigration: FrontEndMigration): FrontEndMigration?
}
