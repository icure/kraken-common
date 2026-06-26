package org.taktik.icure.services.external.rest.v2.dto.conflicts

import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

data class ConflictResolutionResultDto<E : StoredDocumentDto>(
	@ActiveField val document: E,
	@ActiveField val remainingConflicts: List<String> = emptyList()
)