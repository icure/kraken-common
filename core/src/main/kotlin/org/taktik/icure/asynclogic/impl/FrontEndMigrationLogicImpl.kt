/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.FrontEndMigrationDAO
import org.taktik.icure.asynclogic.FrontEndMigrationLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.entities.FrontEndMigration
import org.taktik.icure.validation.aspect.Fixer

@Service
@Profile("app")
class FrontEndMigrationLogicImpl(
	private val frontEndMigrationDAO: FrontEndMigrationDAO,
	datastoreInstanceProvider: org.taktik.icure.datastore.DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters,
) : GenericLogicImpl<FrontEndMigration, FrontEndMigrationDAO>(fixer, datastoreInstanceProvider, filters),
	FrontEndMigrationLogic {
	override suspend fun createFrontEndMigration(frontEndMigration: FrontEndMigration): FrontEndMigration {
		val datastoreInformation = getInstanceAndGroup()
		return frontEndMigrationDAO.create(datastoreInformation, frontEndMigration)
	}

	override suspend fun getFrontEndMigration(frontEndMigrationId: String): FrontEndMigration? {
		val datastoreInformation = getInstanceAndGroup()
		return frontEndMigrationDAO.get(datastoreInformation, frontEndMigrationId)
	}

	override fun getFrontEndMigrationByUserIdName(
		userId: String,
		name: String?,
	): Flow<FrontEndMigration> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(frontEndMigrationDAO.getFrontEndMigrationsByUserIdAndName(datastoreInformation, userId, name))
	}

	override suspend fun modifyFrontEndMigration(frontEndMigration: FrontEndMigration): FrontEndMigration? {
		val datastoreInformation = getInstanceAndGroup()
		return frontEndMigrationDAO.save(datastoreInformation, frontEndMigration)
	}

	override fun getGenericDAO(): FrontEndMigrationDAO = frontEndMigrationDAO
}
