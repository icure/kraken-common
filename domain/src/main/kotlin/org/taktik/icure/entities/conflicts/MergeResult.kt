package org.taktik.icure.entities.conflicts

sealed interface MergeResult {

	data class Success(val id: String, val rev: String): MergeResult
	data class PartialSuccess(val id: String, val rev: String): MergeResult
	data class Failure(val id: String): MergeResult

}