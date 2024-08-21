/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl.filter.code

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.CodeDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.code.CodeByRegionTypeLabelLanguageFilter
import org.taktik.icure.entities.base.Code

@Service
@Profile("app")
class CodeByRegionTypeLabelLanguageFilter(
	private val codeDAO: CodeDAO
) : Filter<String, Code, CodeByRegionTypeLabelLanguageFilter> {
	override fun resolve(
        filter: CodeByRegionTypeLabelLanguageFilter,
        context: Filters,
        datastoreInformation: IDatastoreInformation
    ): Flow<String> = codeDAO.listCodeIdsByLabel(
		datastoreInformation = datastoreInformation,
		region = filter.region,
		language = filter.language,
		type = filter.type,
		label = filter.label
	)
}
