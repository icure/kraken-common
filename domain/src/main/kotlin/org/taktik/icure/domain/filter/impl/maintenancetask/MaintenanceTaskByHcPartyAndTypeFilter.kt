/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.impl.maintenancetask

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.maintenancetask.MaintenanceTaskByHcPartyAndTypeFilter
import org.taktik.icure.entities.MaintenanceTask
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class MaintenanceTaskByHcPartyAndTypeFilter(
	override val desc: String? = null,
	override val healthcarePartyId: String? = null,
	override val type: String,
) : AbstractFilter<MaintenanceTask>,
	MaintenanceTaskByHcPartyAndTypeFilter {

	override val canBeUsedInWebsocket = true

	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = healthcarePartyId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: MaintenanceTask, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = (healthcarePartyId == null || searchKeyMatcher(healthcarePartyId, item)) && type == item.taskType
}
