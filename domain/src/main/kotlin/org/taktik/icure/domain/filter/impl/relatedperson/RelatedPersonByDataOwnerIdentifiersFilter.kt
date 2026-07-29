/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.impl.relatedperson

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.RelatedPerson
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.embed.Identifier

data class RelatedPersonByDataOwnerIdentifiersFilter(
	override val desc: String? = null,
	override val dataOwnerId: String? = null,
	override val identifiers: List<Identifier> = emptyList(),
) : AbstractFilter<RelatedPerson>,
	org.taktik.icure.domain.filter.relatedperson.RelatedPersonByDataOwnerIdentifiersFilter {

	override val canBeUsedInWebsocket = true

	// The data owner id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = dataOwnerId?.let { setOf(it) } ?: emptySet()
	override fun matches(item: RelatedPerson, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = item.endOfLife == null &&
		(dataOwnerId == null || searchKeyMatcher(dataOwnerId, item)) &&
		identifiers.any { searchIdentifier -> item.identifier.any { it.system == searchIdentifier.system && it.id == searchIdentifier.id } }
}
