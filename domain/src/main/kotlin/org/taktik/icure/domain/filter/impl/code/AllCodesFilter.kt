/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.impl.code

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.Filters
import org.taktik.icure.entities.base.Code
import org.taktik.icure.entities.base.Encryptable

data class AllCodesFilter(
	override val desc: String? = null,
) : AbstractFilter<Code>, Filters.AllFilter<String, Code> {

	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Code, searchKeyMatcher: (String, Encryptable) -> Boolean) = true
}
