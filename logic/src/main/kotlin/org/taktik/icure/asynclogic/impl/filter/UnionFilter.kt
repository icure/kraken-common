/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl.filter

import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.Serializable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation

@Service
@Profile("app")
class UnionFilter<T : Serializable, O : Identifiable<T>> :
    Filter<T, O, org.taktik.icure.domain.filter.Filters.UnionFilter<T, O>> {

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun resolve(
        filter: org.taktik.icure.domain.filter.Filters.UnionFilter<T, O>,
        context: Filters,
        datastoreInformation: IDatastoreInformation
    ): Flow<T> {
		return filter.filters.asFlow().flatMapConcat { context.resolve(it, datastoreInformation) }
	}
}
