/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter.patient

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.entities.Patient

@Service
@Profile("app")
class PatientByIdsFilter : Filter<String, Patient, org.taktik.icure.domain.filter.Filters.IdsFilter<String, Patient>> {
	override fun resolve(
        filter: org.taktik.icure.domain.filter.Filters.IdsFilter<String, Patient>,
        context: Filters,
        datastoreInformation: IDatastoreInformation
    ): Flow<String> = filter.ids.asFlow()
}
