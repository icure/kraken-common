package org.taktik.icure.services.external.rest.v2.dto.conflicts

import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto

data class ConflictResolutionResultDto<E : StoredDocumentDto>(
	val document: E,
	val remainingConflicts: List<String> = emptyList()
)