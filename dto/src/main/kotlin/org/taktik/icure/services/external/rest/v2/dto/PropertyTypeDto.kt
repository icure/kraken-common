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
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.constants.PropertyTypeScopeDto
import org.taktik.icure.services.external.rest.v2.dto.embed.TypedValuesTypeDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * DTO representing the definition of a property type, including its value type, scope, and
 * uniqueness constraints.
 */
data class PropertyTypeDto(
	/** The unique identifier of the property type. */
	override val id: String,
	/** The revision identifier for optimistic locking. */
	override val rev: String? = null,
	/** The soft-delete timestamp in epoch milliseconds. */
	override val deletionDate: Long? = null,
	/** The human-readable identifier of this property type. */
	val identifier: String,
	/** The value type of this property type. */
	val type: TypedValuesTypeDto? = null,
	/** The scope in which this property type is applicable. */
	val scope: PropertyTypeScopeDto? = null,
	/** Whether values of this property type must be unique. */
	val unique: Boolean = false,
	/** The identifier of the editor component used to edit this property type. */
	val editor: String? = null,
	/** Whether this property type supports localized values. */
	val localized: Boolean = false,
) : StoredDocumentDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
