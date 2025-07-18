/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.EntityReferenceDAO
import org.taktik.icure.asynclogic.EntityReferenceLogic
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.entities.EntityReference
import org.taktik.icure.validation.aspect.Fixer

@Service
@Profile("app")
class EntityReferenceLogicImpl(
	private val entityReferenceDAO: EntityReferenceDAO,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters
) : GenericLogicImpl<EntityReference, EntityReferenceDAO>(fixer, datastoreInstanceProvider, filters), EntityReferenceLogic {

	override suspend fun getLatest(prefix: String): EntityReference? {
		val datastoreInformation = getInstanceAndGroup()
		return entityReferenceDAO.getLatest(datastoreInformation, prefix)
	}

	override fun getGenericDAO(): EntityReferenceDAO {
		return entityReferenceDAO
	}
}
