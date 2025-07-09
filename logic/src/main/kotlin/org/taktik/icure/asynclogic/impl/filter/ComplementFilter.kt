/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl.filter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toSet
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import java.io.Serializable

@Service
@Profile("app")
class ComplementFilter<T : Serializable, O : Identifiable<T>> :
	Filter<T, O, org.taktik.icure.domain.filter.Filters.ComplementFilter<T, O>> {
	override fun resolve(
		filter: org.taktik.icure.domain.filter.Filters.ComplementFilter<T, O>,
		context: Filters,
		datastoreInformation: IDatastoreInformation
    ): Flow<T> = flow {
		val superFlow: Flow<T> = context.resolve(filter.superSet, datastoreInformation)
		val subList: Set<T> = context.resolve(filter.subSet, datastoreInformation).toSet()
		superFlow.collect {
			if (!subList.contains(it)) emit(it)
		}
	}
}
