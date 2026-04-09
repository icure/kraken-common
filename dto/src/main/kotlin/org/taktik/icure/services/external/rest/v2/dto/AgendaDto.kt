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
import org.taktik.icure.services.external.rest.v2.dto.base.HasEndOfLifeDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AgendaSlottingAlgorithmDto
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
	/** A value used by the appointment availabilities and safe booking features, usually unnecessary but may be required for some complex agendas. The value should be a fuzzy time in HHMMSS format # Motivation When using the appointment availabilities, and safe booking features, iCure requires that the combined working hours of all timetables and timetable items can be split in blocks of at most 24 hours. For example, an agenda with timetable items that give the following 3 working hours: - Monday from 12:00 to 22:00 - Monday from 20:00 to Tuesday 6:00 - Tuesday from 4:00 to 14:00 is not accepted as it creates a contiguous block from 12:00 of Monday to 14:00 of Tuesday (26 hours). It would be acceptable instead to have the following working: - Monday from 12:00 to 22:00 - Monday from 20:00 to Tuesday 5:00 - Tuesday from 5:00 to 14:00 As we have two blocks (M12-T05, T05-T14) each less than 24 hours long If in your agenda the blocks are too long you have two options: either split the agenda in multiple smaller agendas, or provide a value for [daySplitHour], which will force a split in the blocks at the specified time. A `daySplitHour=4_00_00` in the first example would force the split of the block in two blocks: [M12-T4, T4-14] Notes: - The split hour if provided (not null) is always applied, even if not needed. If set to 5_00_00 on the second example, there would be no change, but if set to 4_00_00 we would get 3 groups (M12-T04, T04-T05, T05-T14). - The need for a split is checked only during the execution of appointment availabilities and safe booking methods (if needed but not provided they will fail). Changes to the agenda entity won't check the need for a split hour. # Effect on availabilities and safe booking A [daySplitHour] if not set properly may give availabilities that do not exist. To ensure the correctness of the availabilities and safe booking features you will have to make sure that no calendar items are created across the day split hour (for example if set to 2AM don't have a calendar item from 1:50AM to 2:10AM). The safe booking methods will validate this, but there is no validation done on calendar items created through the unrestricted methods. You could also have issues in cases where the [daySplitHour] is changed without taking appropriate migration steps. */
	val daySplitHour: Int? = null,
	/** If true the agenda won't be available for availabilities and safe booking requests, if false (default) the agenda can be used with those features normally. An unpublished agenda has less strict integrity checks. */
	@param:JsonInclude(JsonInclude.Include.NON_DEFAULT) val unpublished: Boolean = false,
	/** The name of the agenda. */
	val name: String? = null,
	/** The id of the user associated with this agenda. */
	val userId: String? = null,
	/** An identifier for the zone of the agenda. Must be an id accepted by java's ZoneId. Mandatory if the agenda specifies any time-based constraint: - A nested timetable has an item with non-null [EmbeddedTimeTableItem.notAfterInMinutes] - A nested timetable has an item with non-null [EmbeddedTimeTableItem.notBeforeInMinutes] */
	val zoneId: String? = null,
	/** The legacy rights for this agenda. Deprecated: use userRights instead. */
	@Deprecated("Use `userRights` instead") val rights: List<RightDto> = emptyList(),
	/** Associates a user id to the permission that user has on the entity. */
	@param:Schema(description = "Associates a user id to the permission that user has on the entity.")
	@param:JsonInclude(JsonInclude.Include.NON_EMPTY) val userRights: Map<String, UserAccessLevelDto> = emptyMap(),
	/** The algorithm to use for computing time slots in the agenda. */
	val slottingAlgorithm: AgendaSlottingAlgorithmDto? = null,
	/** If not null limits the amount of appointments that each user without special privileges is allowed to take for this agenda during each month. */
	val publicBookingQuota: Int? = null,
	/** Custom properties of the agenda. Public on public agenda. */
	@param:JsonInclude(JsonInclude.Include.NON_EMPTY) val properties: Set<PropertyStubDto> = emptySet(),
	/** Associates a user id to the permission that user has on the entity. / */
	@param:JsonInclude(JsonInclude.Include.NON_EMPTY) val schedules: List<ResourceGroupAllocationScheduleDto> = emptyList(),
) : StoredDocumentDto, ICureDocumentDto<String>, HasEndOfLifeDto {

	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
