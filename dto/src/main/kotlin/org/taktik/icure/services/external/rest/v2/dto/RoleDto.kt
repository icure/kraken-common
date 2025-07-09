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
data class RoleDto(
	override val id: String,
	override val rev: String? = null,
	override val deletionDate: Long? = null,

	override val name: String? = null,
	val permissions: Set<String> = emptySet(),
	val hierarchyIndex: RoleHierarchyLevelDto = RoleHierarchyLevelDto.Child
) : StoredDocumentDto, PrincipalDto, Cloneable, Serializable {
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)

	@get:JsonIgnore
	override val properties: Set<PropertyStubDto> get() = emptySet()

	enum class RoleHierarchyLevelDto { Root, Parent, Child }
}
