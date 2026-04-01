package org.taktik.icure.services.external.rest.v2.mapper.conflicts

import org.springframework.stereotype.Service
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.conflicts.ConflictResolutionResult
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.conflicts.ConflictResolutionResultDto

@Service
class ConflictResolutionV2Mapper {

	suspend fun <I : StoredDocument, O : StoredDocumentDto> map(
		conflictResolutionResult: ConflictResolutionResult<I>,
		documentMapper: suspend (I) -> O
	): ConflictResolutionResultDto<O> =
		ConflictResolutionResultDto(
			document = documentMapper(conflictResolutionResult.document),
			remainingConflicts = conflictResolutionResult.remainingConflicts
		)

}