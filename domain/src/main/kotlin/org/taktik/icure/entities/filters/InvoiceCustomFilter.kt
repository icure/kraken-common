package org.taktik.icure.entities.filters

import org.taktik.icure.entities.Invoice
import org.taktik.icure.entities.dao.KeyComponent
import org.taktik.icure.entities.dao.RangeQueryParameters

data class InvoiceByKeysCustomFilter(
	override val viewName: String,
	override val startKey: List<KeyComponent<*>>?,
	override val startDocumentId: String?,
	override val limit: Int,
	override val keyComponents: List<List<KeyComponent<*>>>
) : ByKeysCustomFilter {

	override val entity: Class<*>
		get() = Invoice::class.java

}

data class InvoiceByRangeCustomFilter(
	override val viewName: String,
	override val startKey: List<KeyComponent<*>>?,
	override val startDocumentId: String?,
	override val limit: Int,
	override val nonRangeKeyComponents: List<List<KeyComponent<*>>>,
	override val range: RangeQueryParameters,
) : ByRangeCustomFilter {

	override val entity: Class<*>
		get() = Invoice::class.java

}