/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.relatedperson

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.RelatedPerson

/**
 * Returns all the [RelatedPerson.id]s with a delegation for [dataOwnerId] where [RelatedPerson.firstName] or
 * [RelatedPerson.lastName] have a word that starts with the specified [name].
 * As this filter explicitly specifies a data owner id, it does not require any security precondition to be used.
 */
interface RelatedPersonByDataOwnerNameFilter : Filter<String, RelatedPerson> {
	val name: String?
	val dataOwnerId: String?
}
