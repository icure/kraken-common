/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.maintenancetask

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.maintenancetask.MaintenanceTaskAfterDateFilter
import org.taktik.icure.entities.MaintenanceTask
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class MaintenanceTaskAfterDateFilter(
	override val desc: String? = null,
	override val healthcarePartyId: String? = null,
	override val date: Long,
) : AbstractFilter<MaintenanceTask>, MaintenanceTaskAfterDateFilter {

	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = healthcarePartyId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: MaintenanceTask, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		return (healthcarePartyId == null || searchKeyMatcher(healthcarePartyId, item)) && (item.created ?: 0) > date
	}
}
