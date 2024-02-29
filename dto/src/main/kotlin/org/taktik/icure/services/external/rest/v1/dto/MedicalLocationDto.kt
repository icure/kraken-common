/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.base.NamedDto
import org.taktik.icure.services.external.rest.v1.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.embed.AddressDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MedicalLocationDto(
	override val id: String,
	override val rev: String? = null,
	override val deletionDate: Long? = null,

	override val name: String? = null,
	val description: String? = null,
	val responsible: String? = null,
	val guardPost: Boolean? = null,
	val cbe: String? = null,
	val bic: String? = null,
	val bankAccount: String? = null,
	val nihii: String? = null,
	val ssin: String? = null,
	val address: AddressDto? = null,
	val agendaIds: Set<String> = emptySet(),
	val options: Map<String, String> = emptyMap(),
	val publicInformations: Map<String, String> = emptyMap(),
	) : StoredDocumentDto, NamedDto {
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
