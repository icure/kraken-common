/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter.maintenancetask

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.entities.MaintenanceTask

@Service
@Profile("app")
class MaintenanceTaskByIdsFilter :
	Filter<String, MaintenanceTask, org.taktik.icure.domain.filter.Filters.IdsFilter<String, MaintenanceTask>> {
	override fun resolve(
        filter: org.taktik.icure.domain.filter.Filters.IdsFilter<String, MaintenanceTask>,
        context: Filters,
        datastoreInformation: IDatastoreInformation
    ): Flow<String> = filter.ids.asFlow()

}
