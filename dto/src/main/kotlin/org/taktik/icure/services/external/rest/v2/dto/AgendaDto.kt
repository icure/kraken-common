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
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AgendaSlottingAlgorithmDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ResourceGroupAllocationScheduleDto
import org.taktik.icure.services.external.rest.v2.dto.embed.RightDto
import org.taktik.icure.services.external.rest.v2.dto.embed.UserAccessLevelDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class AgendaDto(
	override val id: String,
	override val rev: String? = null,
	override val created: Long? = null,
	override val modified: Long? = null,
	override val author: String? = null,
	override val responsible: String? = null,
	override val medicalLocationId: String? = null,
	override val tags: Set<CodeStubDto> = emptySet(),
	override val codes: Set<CodeStubDto> = emptySet(),
	override val endOfLife: Long? = null,
	override val deletionDate: Long? = null,
	val daySplitHour: Int? = null,
	val name: String? = null,
	val userId: String? = null,
	val zoneId: String? = null,
	val lockCalendarItemsBeforeInMinutes: Int? = null,
	@Deprecated("Use `userRights` instead") val rights: List<RightDto> = emptyList(),
	@Schema(description = "Associates a user id to the permission that user has on the entity.")
	@JsonInclude(JsonInclude.Include.NON_NULL) val userRights: Map<String, UserAccessLevelDto> = emptyMap(),
	val slottingAlgorithm: AgendaSlottingAlgorithmDto? = null,
	@JsonInclude(JsonInclude.Include.NON_NULL) val properties: Set<PropertyStubDto> = emptySet(),
	@JsonInclude(JsonInclude.Include.NON_NULL) val schedules: List<ResourceGroupAllocationScheduleDto> = emptyList(),
) : StoredDocumentDto, ICureDocumentDto<String> {
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
