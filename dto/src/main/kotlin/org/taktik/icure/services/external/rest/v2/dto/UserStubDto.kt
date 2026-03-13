/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */
package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.enums.UsersStatusDto
import org.taktik.icure.services.external.rest.v2.dto.enums.UsersTypeDto
import org.taktik.icure.utils.InstantDeserializer
import org.taktik.icure.utils.InstantSerializer
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Lightweight stub representation of a user, containing only the essential identification and
 * status fields. Used when the full user payload is not needed.
 */
data class UserStubDto(
	/** The unique identifier of the user. */
	override val id: String,
	/** The revision identifier for optimistic locking. */
	override val rev: String? = null,
	/** The soft-delete timestamp in epoch milliseconds. */
	override val deletionDate: Long? = null,
	/** The display name of the user. */
	val name: String? = null,
	/** The type of user (e.g. database, external). */
	val type: UsersTypeDto? = null,
	/** The current status of the user (e.g. active, disabled). */
	val status: UsersStatusDto? = null,
	/** The login identifier of the user. */
	val login: String? = null,
	/** The identifier of the group this user belongs to. */
	val groupId: String? = null,
	/** The identifier of the healthcare party linked to this user. */
	val healthcarePartyId: String? = null,
	/** The identifier of the patient linked to this user. */
	val patientId: String? = null,
	/** The email address of the user. */
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
