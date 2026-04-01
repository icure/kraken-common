package org.taktik.icure.services.external.rest.v2.dto.conflicts

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
sealed interface MergeResultDto {

	data class Success(val id: String, val rev: String): MergeResultDto
	data class PartialSuccess(val id: String, val rev: String): MergeResultDto
	data class Failure(val id: String): MergeResultDto

}