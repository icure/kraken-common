/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.impl.insurance

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.Filters
import org.taktik.icure.entities.Insurance
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class AllInsurancesFilter(
	override val desc: String? = null,
) : AbstractFilter<Insurance>,
	Filters.AllFilter<String, Insurance> {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Insurance, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean) = true
}
