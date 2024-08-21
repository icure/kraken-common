/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.contact

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Contact

/**
 * Returns all the [Contact]s where at least one [Contact.services] with id among the specified [ids] exist.
 * As [Contact] is an encryptable entity but this filter does not specify any data owner id, a special permission is
 * needed to use this filter.
 */
interface ContactByServiceIdsFilter : Filter<String, Contact> {
	val ids: List<String>?
}
