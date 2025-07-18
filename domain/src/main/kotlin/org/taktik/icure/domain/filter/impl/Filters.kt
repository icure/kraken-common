/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl

import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.Filter

object Filters {
	fun <O : Identifiable<String>> union(vararg filters: AbstractFilter<O>): UnionFilter<O> = UnionFilter(null, filters.toList())

	fun <O : Identifiable<String>> intersection(vararg filters: AbstractFilter<O>): IntersectionFilter<O> = IntersectionFilter(null, filters.toList())

	fun <O : Identifiable<String>> complement(superSet: AbstractFilter<O>, subset: AbstractFilter<O>): Filter<String, O> = ComplementFilter(null, superSet, subset)
}
