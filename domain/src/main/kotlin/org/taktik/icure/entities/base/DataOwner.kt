package org.taktik.icure.entities.base

import org.taktik.icure.entities.utils.MergeUtil
import org.taktik.icure.mergers.annotations.MergeStrategyUse

interface DataOwner {
	@MergeStrategyUse(
		canMerge = "true",
		merge = "mergeSets({{LEFT}}.{{PROP}}, {{RIGHT}}.{{PROP}})",
		imports = ["org.taktik.icure.entities.utils.MergeUtil.mergeSets"]
	)
	val properties: Set<PropertyStub>

}
