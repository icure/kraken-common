package org.taktik.icure.asynclogic.impl.filter.medicallocation

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.MedicalLocationDAO
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.domain.filter.Filters
import org.taktik.icure.entities.MedicalLocation

@Service
@Profile("app")
class AllMedicalLocationsFilter(
	private val medicalLocationDAO: MedicalLocationDAO
) : Filter<String, MedicalLocation, Filters.AllFilter<String, MedicalLocation>> {

	override fun resolve(
		filter: Filters.AllFilter<String, MedicalLocation>,
		context: org.taktik.icure.asynclogic.impl.filter.Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = medicalLocationDAO.getEntityIds(datastoreInformation)

}
