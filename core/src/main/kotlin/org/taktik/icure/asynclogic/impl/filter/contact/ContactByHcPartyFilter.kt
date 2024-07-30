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

package org.taktik.icure.asynclogic.impl.filter.contact

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.entities.Contact
import org.taktik.icure.utils.mergeUniqueIdsForSearchKeys

@Service
@Profile("app")
class ContactByHcPartyFilter(
    private val contactDAO: ContactDAO,
    private val sessionInformationProvider: SessionInformationProvider
) : Filter<String, Contact, org.taktik.icure.domain.filter.Filters.ByHcpartyFilter<String, Contact>> {
	override fun resolve(
        filter: org.taktik.icure.domain.filter.Filters.ByHcpartyFilter<String, Contact>,
        context: Filters,
        datastoreInformation: IDatastoreInformation
    ): Flow<String> = flow {
        emitAll(
            mergeUniqueIdsForSearchKeys(sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.hcpId)) { key ->
                contactDAO.listContactIdsByHealthcareParty(datastoreInformation, key)
            }
        )
    }
}
