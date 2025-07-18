/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.predicate

import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.domain.filter.predicate.Predicate

data class NotPredicate(val predicate: Predicate) : Predicate {
	override fun apply(input: Identifiable<String>): Boolean = !predicate.apply(input)
}
