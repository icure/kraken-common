/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.invoice

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.Invoice
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class InvoiceByHcPartyCodeDateFilter(
	override val desc: String? = null,
	override val healthcarePartyId: String? = null,
	override val code: String,
	override val startInvoiceDate: Long? = null,
	override val endInvoiceDate: Long? = null,
) : AbstractFilter<Invoice>,
	org.taktik.icure.domain.filter.invoice.InvoiceByHcPartyCodeDateFilter {

	override val canBeUsedInWebsocket = true

	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = healthcarePartyId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: Invoice, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = (healthcarePartyId == null || searchKeyMatcher(healthcarePartyId, item)) &&
		(item.invoicingCodes.any { ic -> code.let { ic.tarificationId?.contains(it) } ?: false }) &&
		(startInvoiceDate == null || item.invoiceDate != null || startInvoiceDate < (item.invoiceDate ?: 0)) &&
		(endInvoiceDate == null || item.invoiceDate != null || (item.invoiceDate ?: 0) > endInvoiceDate)
}
