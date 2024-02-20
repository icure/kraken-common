/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v1.dto.base.StoredDocumentDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CalendarItemTypeDto(
	override val id: String,
	override val rev: String? = null,
	override val deletionDate: Long? = null,
	val name: String? = null,
	val color: String? = null, //"#123456"
	@Schema(defaultValue = "0") val duration: Int = 0,
	val externalRef: String? = null,
	val mikronoId: String? = null,
	val docIds: Set<String> = emptySet(),
	val otherInfos: Map<String, String> = emptyMap(),
	val subjectByLanguage: Map<String, String> = emptyMap()
) : StoredDocumentDto {
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
