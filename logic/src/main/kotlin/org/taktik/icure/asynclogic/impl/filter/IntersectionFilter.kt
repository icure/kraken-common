/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl.filter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.flow.toList
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.datastore.IDatastoreInformation
import java.io.Serializable

@Service
@Profile("app")
class IntersectionFilter<T : Serializable, O : Identifiable<T>> :
	Filter<T, O, org.taktik.icure.domain.filter.Filters.IntersectionFilter<T, O>> {
	override fun resolve(
		filter: org.taktik.icure.domain.filter.Filters.IntersectionFilter<T, O>,
		context: Filters,
		datastoreInformation: IDatastoreInformation
    ): Flow<T> = flow {
		val filters = filter.filters
		val result = LinkedHashSet<T>()
		for (i in filters.indices) {
			if (i == 0) {
				result.addAll(context.resolve(filters[i], datastoreInformation).toList())
			} else {
				result.retainAll(context.resolve(filters[i], datastoreInformation).toCollection(LinkedHashSet()))
			}
		}
		result.forEach { emit(it) } // TODO SH MB: not reactive... can be optimized?
	}
}
