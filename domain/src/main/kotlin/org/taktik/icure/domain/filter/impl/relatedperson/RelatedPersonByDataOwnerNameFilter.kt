/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.impl.relatedperson

import org.taktik.icure.db.sanitizeString
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.RelatedPerson
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class RelatedPersonByDataOwnerNameFilter(
	override val desc: String? = null,
	override val name: String? = null,
	override val dataOwnerId: String? = null,
) : AbstractFilter<RelatedPerson>,
	org.taktik.icure.domain.filter.relatedperson.RelatedPersonByDataOwnerNameFilter {

	override val canBeUsedInWebsocket = true

	// The data owner id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = dataOwnerId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: RelatedPerson, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		val ss = sanitizeString(name)
		return (dataOwnerId == null || searchKeyMatcher(dataOwnerId, item)) &&
			ss != null &&
			sanitizeString((item.lastName ?: "") + (item.firstName ?: ""))!!.contains(ss)
	}
}
