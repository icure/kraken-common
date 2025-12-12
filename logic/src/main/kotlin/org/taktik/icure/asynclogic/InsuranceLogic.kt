/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Insurance
import org.taktik.icure.pagination.PaginationElement

interface InsuranceLogic : EntityPersister<Insurance> {
	suspend fun createInsurance(insurance: Insurance): Insurance

	suspend fun getInsurance(insuranceId: String): Insurance?
	fun listInsurancesByCode(code: String): Flow<Insurance>
	fun listInsurancesByName(name: String): Flow<Insurance>

	suspend fun modifyInsurance(insurance: Insurance): Insurance?
	fun getInsurances(ids: Set<String>): Flow<Insurance>

	/**
	 * Retrieves all the [Insurance]s defined in the group of the current logged-in user in a format for pagination.
	 *
	 * @param paginationOffset a [PaginationOffset] of [Nothing] (i.e. with an always-null start key) for pagination.
	 * @return a [Flow] of [Insurance]s wrapped in [PaginationElement]s for pagination.
	 */
	fun getAllInsurances(paginationOffset: PaginationOffset<Nothing>): Flow<PaginationElement>
}
