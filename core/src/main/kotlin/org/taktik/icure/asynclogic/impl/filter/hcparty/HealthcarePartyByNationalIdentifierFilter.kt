package org.taktik.icure.asynclogic.impl.filter.hcparty

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.HealthcarePartyDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.hcparty.HealthcarePartyByNationalIdentifierFilter
import org.taktik.icure.entities.HealthcareParty

@Service
@Profile("app")
class HealthcarePartyByNationalIdentifierFilter(
	private val healthcarePartyDAO: HealthcarePartyDAO,
) : Filter<String, HealthcareParty, HealthcarePartyByNationalIdentifierFilter> {
	override fun resolve(
		filter: HealthcarePartyByNationalIdentifierFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = healthcarePartyDAO.listHealthcarePartyIdsBySsinOrNihii(
		datastoreInformation = datastoreInformation,
		searchValue = filter.searchValue,
		desc = filter.descending ?: false,
	)
}
