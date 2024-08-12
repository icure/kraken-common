package org.taktik.icure.domain.filter.device

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Device

/**
 * Retrieves all the [Device]s where [Device.responsible] is equal to [responsibleId].
 */
interface DeviceByHcPartyFilter : Filter<String, Device> {
	val responsibleId: String?
}
