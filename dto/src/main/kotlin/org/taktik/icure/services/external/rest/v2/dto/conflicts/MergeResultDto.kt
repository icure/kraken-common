package org.taktik.icure.services.external.rest.v2.dto.conflicts

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
sealed interface MergeResultDto {

		data class Success(@ActiveField val id: String, @ActiveField val rev: String): MergeResultDto
		data class PartialSuccess(@ActiveField val id: String, @ActiveField val rev: String): MergeResultDto
		data class Failure(@ActiveField val id: String): MergeResultDto

}