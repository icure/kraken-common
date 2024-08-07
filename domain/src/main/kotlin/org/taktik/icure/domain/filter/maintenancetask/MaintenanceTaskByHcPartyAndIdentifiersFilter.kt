package org.taktik.icure.domain.filter.maintenancetask

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.MaintenanceTask
import org.taktik.icure.entities.embed.Identifier

/**
 * Retrieves all the [MaintenanceTask.id]s that a data owner can access through their data owner id and their search
 * keys that have in [MaintenanceTask.identifier] at least one of the provided [identifiers].
 * As this filter explicitly specifies a data owner id, it does not require any special permission to be used.
 */
interface MaintenanceTaskByHcPartyAndIdentifiersFilter : Filter<String, MaintenanceTask> {
	val healthcarePartyId: String?
	val identifiers: List<Identifier>
}
