package org.taktik.icure.asynclogic.impl.filter.user

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.UserDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.user.UserByHealthcarePartyIdFilter
import org.taktik.icure.entities.User

@Service
@Profile("app")
class UserByHealthcarePartyIdFilter(
	private val userDAO: UserDAO
) : Filter<String, User, UserByHealthcarePartyIdFilter> {
	override fun resolve(
		filter: UserByHealthcarePartyIdFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = userDAO.listUserIdsByHcpId(datastoreInformation, filter.healthcarePartyId)
}
