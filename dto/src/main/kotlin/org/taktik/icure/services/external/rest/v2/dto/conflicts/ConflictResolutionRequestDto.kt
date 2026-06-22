package org.taktik.icure.services.external.rest.v2.dto.conflicts

import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.conflicts.ConflictResolutionRequestDto")
data class ConflictResolutionRequestDto<E : StoredDocumentDto>(
	@ActiveField val document: E,
	@ActiveField val conflictsToPurge: List<String> = emptyList()
)