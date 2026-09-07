package org.taktik.icure.entities.filters

import org.taktik.icure.entities.MaintenanceTask
import org.taktik.icure.entities.dao.KeyComponent
import org.taktik.icure.entities.dao.RangeQueryParameters

data class MaintenanceTaskByKeysCustomFilter(
	override val viewName: String,
	override val startKey: List<KeyComponent<*>>?,
	override val startDocumentId: String?,
	override val limit: Int,
	override val keyComponents: List<List<KeyComponent<*>>>
) : ByKeysCustomFilter {

	override val entity: Class<*>
		get() = MaintenanceTask::class.java

}

data class MaintenanceTaskByRangeCustomFilter(
	override val viewName: String,
	override val startKey: List<KeyComponent<*>>?,
	override val startDocumentId: String?,
	override val limit: Int,
	override val nonRangeKeyComponents: List<List<KeyComponent<*>>>,
	override val range: RangeQueryParameters,
) : ByRangeCustomFilter {

	override val entity: Class<*>
		get() = MaintenanceTask::class.java

}