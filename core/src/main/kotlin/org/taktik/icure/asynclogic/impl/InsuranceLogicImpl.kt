/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.taktik.icure.asyncdao.InsuranceDAO
import org.taktik.icure.asynclogic.InsuranceLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Insurance
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.validation.aspect.Fixer

class InsuranceLogicImpl(
	private val insuranceDAO: InsuranceDAO,
	private val datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters
) : GenericLogicImpl<Insurance, InsuranceDAO>(fixer, datastoreInstanceProvider, filters), InsuranceLogic {

	override suspend fun getInstanceAndGroup() = datastoreInstanceProvider.getInstanceAndGroup()

	override suspend fun createInsurance(insurance: Insurance) =
		fix(insurance, isCreate = true) { fixedInsurance ->
			if(fixedInsurance.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
			val datastoreInformation = getInstanceAndGroup()
			insuranceDAO.create(datastoreInformation, fixedInsurance)
		}

	override suspend fun getInsurance(insuranceId: String): Insurance? {
		val datastoreInformation = getInstanceAndGroup()
		return insuranceDAO.get(datastoreInformation, insuranceId)
	}

	override fun listInsurancesByCode(code: String): Flow<Insurance> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(insuranceDAO.listInsurancesByCode(datastoreInformation, code))
		}

	override fun listInsurancesByName(name: String): Flow<Insurance> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(insuranceDAO.listInsurancesByName(datastoreInformation, name))
		}

	override suspend fun modifyInsurance(insurance: Insurance) =
		fix(insurance, isCreate = false) { fixedInsurance ->
			val datastoreInformation = getInstanceAndGroup()
			insuranceDAO.save(datastoreInformation, fixedInsurance)
		}

	override fun getInsurances(ids: Set<String>): Flow<Insurance> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(insuranceDAO.getEntities(datastoreInformation, ids))
		}

	override fun getAllInsurances(paginationOffset: PaginationOffset<Nothing>) = flow {
        val datastoreInformation = getInstanceAndGroup()
        emitAll(insuranceDAO
			.getAllInsurances(datastoreInformation, paginationOffset.limitIncludingKey())
	        .toPaginatedFlow<Insurance>(paginationOffset.limit)
		)
	}

	override fun getGenericDAO(): InsuranceDAO {
		return insuranceDAO
	}
}
