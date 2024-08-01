/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.impl

import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.Filters
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class ComplementFilter<O : Identifiable<String>>(
	override val desc: String? = null,
	override val superSet: AbstractFilter<O>,
	override val subSet: AbstractFilter<O>
) : AbstractFilter<O>, Filters.ComplementFilter<String, O> {

	override val canBeUsedInWebsocket = subSet.canBeUsedInWebsocket && superSet.canBeUsedInWebsocket
	override val requiresSecurityPrecondition: Boolean =
		subSet.requiresSecurityPrecondition || superSet.requiresSecurityPrecondition
	override fun requestedDataOwnerIds(): Set<String> =
		listOf(subSet, superSet).flatMap {
			it.requestedDataOwnerIds()
		}.toSet()

	override fun matches(item: O, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		return superSet.matches(item, searchKeyMatcher) && !subSet.matches(item, searchKeyMatcher)
	}
}
