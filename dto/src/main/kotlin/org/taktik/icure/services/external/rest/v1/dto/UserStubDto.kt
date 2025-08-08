/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.taktik.icure.services.external.rest.v1.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.enums.UsersStatusDto
import org.taktik.icure.services.external.rest.v1.dto.enums.UsersTypeDto
import org.taktik.icure.utils.InstantDeserializer
import org.taktik.icure.utils.InstantSerializer
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class UserStubDto(
	override val id: String,
	override val rev: String? = null,
	override val deletionDate: Long? = null,
	val name: String? = null,
	val type: UsersTypeDto? = null,
	val status: UsersStatusDto? = null,
	val login: String? = null,
	val groupId: String? = null,
	val healthcarePartyId: String? = null,
	val patientId: String? = null,
	@param:JsonSerialize(using = InstantSerializer::class)
	@param:JsonInclude(JsonInclude.Include.NON_NULL)
	@param:JsonDeserialize(using = InstantDeserializer::class)
	val email: String? = null,
) : StoredDocumentDto,
	Cloneable,
	Serializable {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
