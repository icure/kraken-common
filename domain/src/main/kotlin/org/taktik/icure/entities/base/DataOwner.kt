package org.taktik.icure.entities.base

import org.taktik.icure.entities.utils.MergeUtil
import org.taktik.icure.mergers.annotations.MergeStrategyUseReference

interface DataOwner {
	@MergeStrategyUseReference("org.taktik.icure.entities.utils.MergeUtil.mergeSets")
	val properties: Set<PropertyStub>

	fun solveConflictsWith(other: DataOwner): Map<String, Any?> = mapOf(
		"properties" to MergeUtil.mergeSets(this.properties, other.properties),
	)
}
