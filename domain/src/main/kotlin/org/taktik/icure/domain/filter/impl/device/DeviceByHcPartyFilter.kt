package org.taktik.icure.domain.filter.impl.device

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.Device
import org.taktik.icure.entities.base.Encryptable

data class DeviceByHcPartyFilter(
	override val desc: String? = null,
	override val responsibleId: String? = null
) : AbstractFilter<Device>, org.taktik.icure.domain.filter.device.DeviceByHcPartyFilter {

	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = responsibleId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: Device, searchKeyMatcher: (String, Encryptable) -> Boolean): Boolean {
		return item.responsible == responsibleId
	}
}
