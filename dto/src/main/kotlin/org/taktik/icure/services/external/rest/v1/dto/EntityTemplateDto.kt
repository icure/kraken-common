/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.base.StoredDocumentDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EntityTemplateDto(
	override val id: String,
	override val rev: String? = null,
	override val deletionDate: Long? = null,

	var userId: String? = null,
	val descr: String? = null,
	val keywords: Set<String>? = null,
	var entityType: String? = null,
	var subType: String? = null,
	var defaultTemplate: Boolean? = null,
	var entity: List<Map<String, Any>> = emptyList()
) : StoredDocumentDto {
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
