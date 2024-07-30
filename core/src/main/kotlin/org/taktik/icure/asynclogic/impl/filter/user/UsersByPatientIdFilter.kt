package org.taktik.icure.asynclogic.impl.filter.user

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.UserDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.user.UsersByPatientIdFilter
import org.taktik.icure.entities.User

@Service
@Profile("app")
class UsersByPatientIdFilter(
	private val userDAO: UserDAO
) : Filter<String, User, UsersByPatientIdFilter> {
	override fun resolve(
		filter: UsersByPatientIdFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = userDAO.listUsersByPatientId(datastoreInformation, filter.patientId).mapNotNull { it.id }
}
