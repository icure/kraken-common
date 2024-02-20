/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v1.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.embed.FrontEndMigrationStatusDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FrontEndMigrationDto(
	override val id: String,
	override val rev: String? = null,
	override val deletionDate: Long? = null,

	val name: String? = null,
	val startDate: Long? = null,
	val endDate: Long? = null,
	val status: FrontEndMigrationStatusDto? = null,
	val logs: String? = null,
	val userId: String? = null,
	val startKey: String? = null,
	val startKeyDocId: String? = null,
	val processCount: Long? = null,

	@Schema(description = "Extra properties for the fem. Those properties are typed (see class Property)") val properties: Set<PropertyStubDto> = emptySet(),

) : StoredDocumentDto {
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
