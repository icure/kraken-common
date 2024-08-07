/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.maintenancetask

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.MaintenanceTask

/**
 * Retrieves all the [MaintenanceTask.id]s that a data owner can access through their data owner id where [MaintenanceTask.taskType]
 * is equal to [type].
 * As this filter explicitly specifies a data owner id, it does not require any special permission to be used.
 */
interface MaintenanceTaskByHcPartyAndTypeFilter : Filter<String, MaintenanceTask> {
	val type: String
	val healthcarePartyId: String?
}
