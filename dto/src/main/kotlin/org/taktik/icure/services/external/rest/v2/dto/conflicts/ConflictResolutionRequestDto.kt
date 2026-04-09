package org.taktik.icure.services.external.rest.v2.dto.conflicts

import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto

data class ConflictResolutionRequestDto<E : StoredDocumentDto>(
	val document: E,
	val conflictsToPurge: List<String> = emptyList()
)