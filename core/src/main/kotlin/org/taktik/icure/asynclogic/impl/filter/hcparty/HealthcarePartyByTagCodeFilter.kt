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
package org.taktik.icure.asynclogic.impl.filter.hcparty

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toSet
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.HealthcarePartyDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.hcparty.HealthcarePartyByTagCodeFilter
import org.taktik.icure.entities.HealthcareParty
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class HealthcarePartyByTagCodeFilter(
	private val healthcarePartyDAO: HealthcarePartyDAO,
) : Filter<String, HealthcareParty, HealthcarePartyByTagCodeFilter> {

	override fun resolve(
        filter: HealthcarePartyByTagCodeFilter,
        context: Filters,
        datastoreInformation: IDatastoreInformation,
	): Flow<String> = flow {
		try {
			val idsByTag = if (filter.tagType != null) {
				healthcarePartyDAO.listHealthcarePartyIdsByTag(
					datastoreInformation = datastoreInformation,
					tagType = filter.tagType!!,
					tagCode = filter.tagCode
				).toSet()
			} else {
				null
			}

			val idsByCode = if (filter.codeType != null) {
				healthcarePartyDAO.listHealthcarePartyIdsByCode(
					datastoreInformation = datastoreInformation,
					codeType = filter.codeType!!,
					codeCode = filter.codeCode
				).toSet()
			} else {
				null
			}

			val ids = when {
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
