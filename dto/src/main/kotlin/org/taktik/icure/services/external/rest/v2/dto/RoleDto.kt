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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.base.PrincipalDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * The RoleDto class represents a role in the system, which can have permissions and can be inheritable up to a certain level by users in child Groups.$
 * @property id The unique identifier of the role.
 * @property rev The revision identifier of the role, used for optimistic locking.
 * @property deletionDate The timestamp of when the role was deleted, if applicable.
 * @property name The name of the role.
 * @property inheritableUpTo The maximum level of inheritance for this role, indicating how far
 * down the group hierarchy this role can be inherited by users in child groups. A value of 0 means it cannot be inherited, while a value of -1 means it can be inherited indefinitely.
 * @property permissions A set of permissions associated with this role, defining what actions users with this role can perform.
 */
data class RoleDto(
	override val id: String,
	override val rev: String? = null,
	override val deletionDate: Long? = null,
	override val name: String? = null,
	val inheritableUpTo: Int? = null,
	val permissions: Set<String> = emptySet(),
) : StoredDocumentDto,
	PrincipalDto,
	Cloneable,
	Serializable {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)

	@get:JsonIgnore
	override val properties: Set<PropertyStubDto> get() = emptySet()
}
