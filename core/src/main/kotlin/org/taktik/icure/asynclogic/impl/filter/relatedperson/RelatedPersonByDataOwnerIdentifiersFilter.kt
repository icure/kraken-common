/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl.filter.relatedperson

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.RelatedPersonDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.relatedperson.RelatedPersonByDataOwnerIdentifiersFilter
import org.taktik.icure.entities.RelatedPerson
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class RelatedPersonByDataOwnerIdentifiersFilter(
	private val relatedPersonDAO: RelatedPersonDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, RelatedPerson, RelatedPersonByDataOwnerIdentifiersFilter> {
	override val entity get() = relatedPersonDAO.entityClass
	override val views = listOf("by_all_delegates_identifier")

	override fun resolve(
		filter: RelatedPersonByDataOwnerIdentifiersFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		try {
			val dataOwnerId = requireNotNull(filter.dataOwnerId ?: sessionLogic.getCurrentDataOwnerIdOrNull()) {
				"A RelatedPersonByDataOwnerIdentifiersFilter must either provide an explicit dataOwnerId or must be used by a data owner user"
			}
			emitAll(
				relatedPersonDAO.listRelatedPersonIdsByDataOwnerAndIdentifiers(
					datastoreInformation = datastoreInformation,
					searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(dataOwnerId),
					identifiers = filter.identifiers,
				),
			)
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
