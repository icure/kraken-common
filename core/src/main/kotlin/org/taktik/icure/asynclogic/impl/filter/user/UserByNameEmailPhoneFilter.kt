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
package org.taktik.icure.asynclogic.impl.filter.user

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.UserDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.user.UserByNameEmailPhoneFilter
import org.taktik.icure.entities.User

@Service
@Profile("app")
class UserByNameEmailPhoneFilter(
	private val userDAO: UserDAO,
) : Filter<String, User, UserByNameEmailPhoneFilter> {
	override fun resolve(
		filter: UserByNameEmailPhoneFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = userDAO.listUserIdsByNameEmailPhone(datastoreInformation, filter.searchString)
}
