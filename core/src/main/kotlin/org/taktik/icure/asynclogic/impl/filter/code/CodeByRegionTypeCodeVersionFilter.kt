package org.taktik.icure.asynclogic.impl.filter.code

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.CodeDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.code.CodeByRegionTypeCodeVersionFilter
import org.taktik.icure.entities.base.Code

@Service
@Profile("app")
class CodeByRegionTypeCodeVersionFilter(
	private val codeDAO: CodeDAO,
) : Filter<String, Code, CodeByRegionTypeCodeVersionFilter> {
	override fun resolve(
		filter: CodeByRegionTypeCodeVersionFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = codeDAO.listCodeIdsBy(
		datastoreInformation = datastoreInformation,
		region = filter.region,
		type = filter.type,
		code = filter.code,
		version = filter.version,
	)
}
