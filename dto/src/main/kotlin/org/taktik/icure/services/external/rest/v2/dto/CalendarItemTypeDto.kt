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
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CalendarItemTypeDto(
	override val id: String,
	override val rev: String? = null,
	override val deletionDate: Long? = null,
	val healthcarePartyId: String? = null,
	val agendaId: String? = null,
	val defaultCalendarItemType: Boolean = false,
	val name: String? = null,
	val color: String? = null, // "#123456"
	@Schema(defaultValue = "0") val duration: Int = 0,
	val externalRef: String? = null,
	val mikronoId: String? = null,
	val docIds: Set<String> = emptySet(),
	val otherInfos: Map<String, String> = emptyMap(),
	val subjectByLanguage: Map<String, String> = emptyMap(),
) : StoredDocumentDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
