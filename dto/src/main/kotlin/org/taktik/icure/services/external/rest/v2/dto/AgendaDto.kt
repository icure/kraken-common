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
import org.taktik.icure.CardinalMetadataProperty
import org.taktik.icure.entities.RawJson
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEndOfLifeDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AgendaSlottingAlgorithmDto
import org.taktik.icure.services.external.rest.v2.dto.base.ExtendableRootDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ResourceGroupAllocationScheduleDto
import org.taktik.icure.services.external.rest.v2.dto.embed.RightDto
import org.taktik.icure.services.external.rest.v2.dto.embed.UserAccessLevelDto

@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents an agenda that keeps track of appointments (calendar items) for a resource or group of resources.
 * An agenda can specify a schedule for its resources and allows managing availabilities for booking.
 */
data class AgendaDto(
	/** The Id of the agenda. We encourage using either a v4 UUID or a HL7 Id. */
	override val id: String,
	/** The revision of the agenda in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation of this entity. */
	override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification of this entity. */
	override val modified: Long? = null,
	/** The id of the User that created this agenda. */
	override val author: String? = null,
	/** The id of the data owner that is responsible for this agenda. */
	override val responsible: String? = null,
	/** The medical location where this entity was created. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** Tags that qualify the agenda as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this particular agenda. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	override val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** A fuzzy time in HHMMSS format used to split working hours into blocks for availabilities computation. */
	@CardinalMetadataProperty
	val daySplitHour: Int? = null,
	/** If true the agenda is not available for availabilities and safe booking requests. */
	@CardinalMetadataProperty
	@param:JsonInclude(JsonInclude.Include.NON_DEFAULT) val unpublished: Boolean = false,
	/** The name of the agenda. */
	val name: String? = null,
	/** The id of the user associated with this agenda. */
	val userId: String? = null,
	/** An identifier for the time zone of the agenda, must be an id accepted by java's ZoneId. */
	@CardinalMetadataProperty
	val zoneId: String? = null,
	/** The legacy rights for this agenda. Deprecated: use userRights instead. */
	@Deprecated("Use `userRights` instead")
	@CardinalMetadataProperty
	val rights: List<RightDto> = emptyList(),
	/** Associates a user id to the permission that user has on the entity. */
	@param:Schema(description = "Associates a user id to the permission that user has on the entity.")
	@CardinalMetadataProperty
	@param:JsonInclude(JsonInclude.Include.NON_EMPTY) val userRights: Map<String, UserAccessLevelDto> = emptyMap(),
	/** The algorithm to use for computing time slots in the agenda. */
	@CardinalMetadataProperty
	val slottingAlgorithm: AgendaSlottingAlgorithmDto? = null,
	/** If not null, limits the amount of monthly appointments per unprivileged user for this agenda. */
	@CardinalMetadataProperty
	val publicBookingQuota: Int? = null,
	/** Custom properties of the agenda. */
	@param:JsonInclude(JsonInclude.Include.NON_EMPTY) val properties: Set<PropertyStubDto> = emptySet(),
	/** The resource group allocation schedules defining availability rules for this agenda. */
	@CardinalMetadataProperty
	@param:JsonInclude(JsonInclude.Include.NON_EMPTY) val schedules: List<ResourceGroupAllocationScheduleDto> = emptyList(),
	override val extensions: RawJson.JsonObject? = null,
	override val extensionsVersion: Int? = null,
) : StoredDocumentDto,
	ICureDocumentDto<String>,
	HasEndOfLifeDto,
	ExtendableRootDto {

	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
