/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.embed.AddressDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class InsuranceDto(
	override val id: String,
	override val rev: String? = null,
	override val deletionDate: Long? = null,

	val name: Map<String, String> = emptyMap(),
	val privateInsurance: Boolean = false,
	val hospitalisationInsurance: Boolean = false,
	val ambulatoryInsurance: Boolean = false,
	val code: String? = null,
	val agreementNumber: String? = null,
	val parent: String? = null, //ID of the parent
	val address: AddressDto = AddressDto()
) : StoredDocumentDto {
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
