/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.base.IdentifiableDto
import org.taktik.icure.services.external.rest.v1.dto.base.NamedDto
import org.taktik.icure.services.external.rest.v1.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DatabaseSynchronizationDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ReplicationDto(
	override val id: String,
	override val rev: String? = null,
	override val deletionDate: Long? = null,

	override val name: String? = null,
	var context: String? = null,
	var databaseSynchronizations: List<DatabaseSynchronizationDto> = emptyList()
) : StoredDocumentDto, IdentifiableDto<String>, NamedDto {
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
