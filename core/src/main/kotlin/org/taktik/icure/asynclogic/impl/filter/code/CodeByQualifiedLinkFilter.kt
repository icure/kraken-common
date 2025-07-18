package org.taktik.icure.asynclogic.impl.filter.code

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.CodeDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.code.CodeByQualifiedLinkFilter
import org.taktik.icure.entities.base.Code

@Service
@Profile("app")
class CodeByQualifiedLinkFilter(
	private val codeDAO: CodeDAO,
) : Filter<String, Code, CodeByQualifiedLinkFilter> {
	override fun resolve(
		filter: CodeByQualifiedLinkFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = codeDAO.listCodeIdsByQualifiedLinkId(
		datastoreInformation = datastoreInformation,
		linkType = filter.linkType,
		linkedId = filter.linkedId,
	)
}
