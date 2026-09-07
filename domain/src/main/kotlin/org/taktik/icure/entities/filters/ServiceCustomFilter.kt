package org.taktik.icure.entities.filters

import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.dao.KeyComponent
import org.taktik.icure.entities.dao.RangeQueryParameters

data class ServiceByKeysCustomFilter(
	override val viewName: String,
	override val startKey: List<KeyComponent<*>>?,
	override val startDocumentId: String?,
	override val limit: Int,
	override val keyComponents: List<List<KeyComponent<*>>>
) : ByKeysCustomFilter {

	// This is intentional: a view on Service is ultimately a view on Contact
	override val entity: Class<*>
		get() = Contact::class.java

}

data class ServiceByRangeCustomFilter(
	override val viewName: String,
	override val startKey: List<KeyComponent<*>>?,
	override val startDocumentId: String?,
	override val limit: Int,
	override val nonRangeKeyComponents: List<List<KeyComponent<*>>>,
	override val range: RangeQueryParameters,
) : ByRangeCustomFilter {

	// This is intentional: a view on Service is ultimately a view on Contact
	override val entity: Class<*>
		get() = Contact::class.java

}