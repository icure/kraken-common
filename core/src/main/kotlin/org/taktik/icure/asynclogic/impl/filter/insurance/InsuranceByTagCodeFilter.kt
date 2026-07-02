/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */
package org.taktik.icure.asynclogic.impl.filter.insurance

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toSet
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.InsuranceDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.insurance.InsuranceByTagCodeFilter
import org.taktik.icure.entities.Insurance
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class InsuranceByTagCodeFilter(
	private val insuranceDAO: InsuranceDAO,
) : Filter<String, Insurance, InsuranceByTagCodeFilter> {
	override val entity get() = insuranceDAO.entityClass
	override val views = listOf("by_tags", "by_codes")

	override fun resolve(
		filter: InsuranceByTagCodeFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = flow {
		try {
			val idsByTag =
				if (filter.tagType != null) {
					insuranceDAO
						.listInsuranceIdsByTag(
							datastoreInformation = datastoreInformation,
							tagType = filter.tagType!!,
							tagCode = filter.tagCode,
						).toSet()
				} else {
					null
				}

			val idsByCode =
				if (filter.codeType != null) {
					insuranceDAO
						.listInsuranceIdsByCode(
							datastoreInformation = datastoreInformation,
							codeType = filter.codeType!!,
							codeCode = filter.codeCode,
						).toSet()
				} else {
					null
				}

			val ids =
				when {
					idsByTag != null && idsByCode != null -> idsByTag.intersect(idsByCode).asFlow()
					idsByTag != null -> idsByTag.asFlow()
					idsByCode != null -> idsByCode.asFlow()
					else -> emptyFlow()
				}

			emitAll(ids)
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
