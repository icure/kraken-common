/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.maintenancetask

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.Filters
import org.taktik.icure.entities.MaintenanceTask
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class MaintenanceTaskByIdsFilter(
	override val desc: String? = null,
	override val ids: Set<String>,
) : AbstractFilter<MaintenanceTask>,
	Filters.IdsFilter<String, MaintenanceTask> {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: MaintenanceTask, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean) = ids.contains(item.id)
}
