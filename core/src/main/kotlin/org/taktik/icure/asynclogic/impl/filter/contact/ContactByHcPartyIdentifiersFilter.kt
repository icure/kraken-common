package org.taktik.icure.asynclogic.impl.filter.contact

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.contact.ContactByHcPartyIdentifiersFilter
import org.taktik.icure.entities.Contact
import org.taktik.icure.utils.getLoggedHealthCarePartyId
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class ContactByHcPartyIdentifiersFilter(
	private val contactDAO: ContactDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, Contact, ContactByHcPartyIdentifiersFilter> {

	override fun resolve(
        filter: ContactByHcPartyIdentifiersFilter,
        context: Filters,
        datastoreInformation: IDatastoreInformation
    ): Flow<String> = flow {
		try {
			val hcPartyId = filter.healthcarePartyId ?: getLoggedHealthCarePartyId(sessionLogic)
			emitAll(contactDAO.listContactIdsByHcPartyAndIdentifiers(
				datastoreInformation = datastoreInformation,
				searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(hcPartyId),
				identifiers = filter.identifiers
			))
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
