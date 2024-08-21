package org.taktik.icure.asynclogic.impl.filter.code

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.CodeDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.code.CodeIdsByTypeCodeVersionIntervalFilter
import org.taktik.icure.entities.base.Code

@Service
@Profile("app")
class CodeIdsByTypeCodeVersionIntervalFilter(
	private val codeDAO: CodeDAO,
) : Filter<String, Code, CodeIdsByTypeCodeVersionIntervalFilter> {

	override fun resolve(
        filter: CodeIdsByTypeCodeVersionIntervalFilter,
        context: Filters,
        datastoreInformation: IDatastoreInformation
    ): Flow<String> {
		return codeDAO.listCodeIdsByTypeCodeVersionInterval(
			datastoreInformation,
			filter.startType,
			filter.startCode,
			filter.startVersion,
			filter.endType,
			filter.endCode,
			filter.endVersion
		)
	}
}
