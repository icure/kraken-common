/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter.relatedperson

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.RelatedPerson

@Service
@Profile("app")
class RelatedPersonByIdsFilter : Filter<String, RelatedPerson, org.taktik.icure.domain.filter.Filters.IdsFilter<String, RelatedPerson>> {
	override fun resolve(
		filter: org.taktik.icure.domain.filter.Filters.IdsFilter<String, RelatedPerson>,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = filter.ids.asFlow()
}
