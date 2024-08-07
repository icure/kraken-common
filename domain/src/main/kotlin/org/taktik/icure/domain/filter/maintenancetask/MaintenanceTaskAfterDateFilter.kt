/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.maintenancetask

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.MaintenanceTask

/**
 * Retrieves all the [MaintenanceTask.id]s for which [healthcarePartyId] has a delegation where [MaintenanceTask.created]
 * is not null and greater than [date].
 * As this filter explicitly specifies a data owner id, it does not require any special permission to be used.
 */
interface MaintenanceTaskAfterDateFilter : Filter<String, MaintenanceTask> {
	val healthcarePartyId: String?
	val date: Long
}
