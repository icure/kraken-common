/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.relatedperson

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.Filters
import org.taktik.icure.entities.RelatedPerson
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class RelatedPersonByIdsFilter(
	override val ids: Set<String>,
	override val desc: String? = null,
) : AbstractFilter<RelatedPerson>,
	Filters.IdsFilter<String, RelatedPerson> {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: RelatedPerson, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean) = ids.contains(item.id)
}
