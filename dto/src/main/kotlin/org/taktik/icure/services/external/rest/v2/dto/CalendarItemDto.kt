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
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.icure.entities.RawJson
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEncryptionMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AddressDto
import org.taktik.icure.services.external.rest.v2.dto.embed.CalendarItemTagDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EncryptableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ExtendableRootDto
import org.taktik.icure.services.external.rest.v2.dto.embed.FlowItemDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents an appointment or event in a calendar. Calendar items are linked to an agenda and can block
 * availabilities for scheduling purposes.
 */
data class CalendarItemDto(
	/** The Id of the calendar item. We encourage using either a v4 UUID or a HL7 Id. */
	override val id: String,
	/** The revision of the calendar item in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation of this entity. */
	override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification of this entity. */
	override val modified: Long? = null,
	/** The id of the User that created this calendar item. */
	override val author: String? = null,
	/** The id of the data owner that is responsible for this calendar item. */
	override val responsible: String? = null,
	/** The medical location where this entity was created. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** Tags that qualify the calendar item as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this particular calendar item. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** The title of the calendar item. */
	val title: String? = null,
	/** The id of the calendar item type associated with this item. */
	val calendarItemTypeId: String? = null,
	/** The id of the master calendar item if this is a recurring instance. */
	val masterCalendarItemId: String? = null,
	/** The patient id. Deprecated: use cryptedForeignKeys instead. */
	@Deprecated("Use crypedForeignKeys instead")
	val patientId: String? = null,
	/** Whether this calendar item is marked as important. */
	val important: Boolean? = null,
	/** Whether this calendar item represents a home visit. */
	val homeVisit: Boolean? = null,
	/** A phone number associated with this calendar item. */
	val phoneNumber: String? = null,
	/** The id of the place where the appointment takes place. */
	val placeId: String? = null,
	/** The address where the appointment takes place. */
	val address: AddressDto? = null,
	/** The textual representation of the address. */
	val addressText: String? = null,
	/** The start time of the calendar item in YYYYMMDDHHMMSS format. */
	val startTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	/** The end time of the calendar item in YYYYMMDDHHMMSS format. */
	val endTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	/** The time of confirmation in YYYYMMDDHHMMSS format. */
	@Deprecated("Ignored by availabilities algorithm, will be replaced by another more descriptive field")
	val confirmationTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	/** The timestamp of cancellation. */
	@Deprecated("Ignored by availabilities algorithm, will be replaced by another more descriptive field")
	val cancellationTimestamp: Long? = null,
	/** An id associated with the confirmation. */
	@Deprecated("Ignored by availabilities algorithm, will be replaced by another more descriptive field")
	val confirmationId: String? = null,
	/** The duration of the appointment. */
	@Deprecated("Ignored by availabilities algorithm, use appropriate startTime and endTime")
	val duration: Long? = null,
	/** Whether this calendar item spans the entire day. */
	@Deprecated("Ignored by availabilities algorithm, use appropriate startTime and endTime")
	val allDay: Boolean? = null,
	/** Additional details about the calendar item. */
	val details: String? = null,
	/** Whether this calendar item was migrated from another system. */
	val wasMigrated: Boolean? = null,
	/** The id of the agenda linked to this calendar item. */
	val agendaId: String? = null,
	/** The resource group of the agenda that will handle this calendar item. */
	val resourceGroup: CodeStubDto? = null,
	/** How this calendar item is considered by the availabilities algorithm. */
	val availabilitiesAssignmentStrategy: AvailabilitiesAssignmentStrategy? = null,
	/** The healthcare party id associated with this calendar item. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val hcpId: String? = null,
	/** An id for linking recurring calendar items. */
	val recurrenceId: String? = null,
	/** Tags for the meeting associated with this calendar item. */
	val meetingTags: Set<CalendarItemTagDto> = emptySet(),
	/** Flow item information associated with this calendar item. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val flowItem: FlowItemDto? = null,
	/** Custom properties of this calendar item. */
	@param:JsonInclude(JsonInclude.Include.NON_EMPTY) val properties: Set<PropertyStubDto> = emptySet(),
	/** The secret foreign keys, used for secure linking to patients. */
	override val secretForeignKeys: Set<String> = emptySet(),
	/** The encrypted foreign keys. */
	override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The delegations giving access to connected healthcare information. */
	override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The encryption keys used to encrypt secured properties, encrypted for separate Crypto Actors. */
	override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The base64-encoded encrypted fields of this calendar item. */
	override val encryptedSelf: Base64StringDto? = null,
	/** The security metadata of this entity, for access control. */
	override val securityMetadata: SecurityMetadataDto? = null,
	override val extensions: RawJson.JsonObject? = null,
	override val extensionsVersion: Int? = null,
) : StoredDocumentDto,
	ICureDocumentDto<String>,
	HasEncryptionMetadataDto,
	EncryptableDto,
	ExtendableRootDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)

	enum class AvailabilitiesAssignmentStrategy {
		@JsonProperty("S")
		Strict,

		@JsonProperty("L")
		Loose,
	}
}
