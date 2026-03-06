package org.taktik.icure.entities.conflicts

import org.taktik.icure.entities.base.StoredDocument

data class ConflictResolutionResult<E : StoredDocument>(
	val document: E,
	val remainingConflicts: List<String>
)
