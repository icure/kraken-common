/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter

import org.taktik.couchdb.id.Identifiable
import java.io.Serializable

interface Filters {

	interface IdsFilter<T : Serializable, O : Identifiable<T>> : Filter<T, O> {
		val ids: Set<T>
	}

	interface UnionFilter<T : Serializable, O : Identifiable<T>> : Filter<T, O> {
		val filters: List<Filter<T, O>>
	}

	interface IntersectionFilter<T : Serializable, O : Identifiable<T>> : Filter<T, O> {
		val filters: List<Filter<T, O>>
	}

	interface ComplementFilter<T : Serializable, O : Identifiable<T>> : Filter<T, O> {
		val superSet: Filter<T, O>
		val subSet: Filter<T, O>
	}

	interface AllFilter<T : Serializable, O : Identifiable<T>> : Filter<T, O>

	interface ByHcpartyFilter<T : Serializable, O : Identifiable<T>> : Filter<T, O> {
		val hcpId: String
	}
}
