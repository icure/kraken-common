/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.impl.pricing

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.Filters
import org.taktik.icure.entities.Tarification
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class AllPricingFilter(
	override val desc: String? = null,
) : AbstractFilter<Tarification>,
	Filters.AllFilter<String, Tarification> {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Tarification, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean) = true
}
