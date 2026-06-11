package org.taktik.icure.asynclogic.impl.filter.hcparty

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.HealthcarePartyDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.ConfigurationView
import org.taktik.icure.domain.filter.hcparty.HealthcarePartyByTypeSpecialtyPostCodeFilter
import org.taktik.icure.entities.HealthcareParty

@Service
@Profile("app")
class HealthcarePartyByTypeSpecialtyPostCodeFilter(
	private val healthcarePartyDAO: HealthcarePartyDAO,
) : Filter<String, HealthcareParty, HealthcarePartyByTypeSpecialtyPostCodeFilter> {
	override val configurationViews = listOf(ConfigurationView("HealthcareParty", "by_speciality_postcode"))

	override fun resolve(
		filter: HealthcarePartyByTypeSpecialtyPostCodeFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = healthcarePartyDAO.listHealthcarePartyIdsBySpecialityAndPostcode(
		datastoreInformation = datastoreInformation,
		type = filter.specialty,
		spec = filter.specCode,
		firstCode = filter.startPostCode,
		lastCode = filter.endPostCode,
	)
}
