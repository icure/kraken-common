/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.relatedperson

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.RelatedPerson
import org.taktik.icure.entities.embed.Identifier

/**
 * Retrieves all the [RelatedPerson]s with a delegation to [dataOwnerId] and that have at least
 * one of the provided [identifiers] in [RelatedPerson.identifier].
 * As this filter explicitly specifies a data owner id, it does not require any security precondition to be used.
 */
interface RelatedPersonByDataOwnerIdentifiersFilter : Filter<String, RelatedPerson> {
	val dataOwnerId: String?
	val identifiers: List<Identifier>
}
