package org.taktik.icure.entities.filters

import org.taktik.icure.entities.RelatedPerson
import org.taktik.icure.entities.dao.KeyComponent
import org.taktik.icure.entities.dao.RangeQueryParameters
import org.taktik.icure.entities.dao.ValueFilterParameters

data class RelatedPersonByKeysCustomFilter(
	override val viewName: String,
	override val startKey: List<KeyComponent>?,
	override val startDocumentId: String?,
	override val limit: Int,
	override val keyComponents: List<List<KeyComponent>>,
	override val valueFilterParameters: ValueFilterParameters?
) : ByKeysCustomFilter {

	override val entity: Class<*>
		get() = RelatedPerson::class.java

}

data class RelatedPersonByRangeCustomFilter(
	override val viewName: String,
	override val startKey: List<KeyComponent>?,
	override val startDocumentId: String?,
	override val limit: Int,
	override val nonRangeKeyComponents: List<List<KeyComponent>>,
	override val range: RangeQueryParameters,
) : ByRangeCustomFilter {

	override val entity: Class<*>
		get() = RelatedPerson::class.java

}