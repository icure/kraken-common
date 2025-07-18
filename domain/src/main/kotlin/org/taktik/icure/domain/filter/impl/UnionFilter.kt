/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.impl

import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.Filters
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class UnionFilter<O : Identifiable<String>>(
	override val desc: String? = null,
	override val filters: List<AbstractFilter<O>> = listOf(),
) : AbstractFilter<O>,
	Filters.UnionFilter<String, O> {

	override val canBeUsedInWebsocket = filters.all { it.canBeUsedInWebsocket }
	override val requiresSecurityPrecondition: Boolean = filters.any { it.requiresSecurityPrecondition }
	override fun requestedDataOwnerIds(): Set<String> = filters.flatMap { it.requestedDataOwnerIds() }.toSet()

	override fun matches(item: O, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		for (f in filters) {
			if (f.matches(item, searchKeyMatcher)) {
				return true
			}
		}
		return false
	}
}
