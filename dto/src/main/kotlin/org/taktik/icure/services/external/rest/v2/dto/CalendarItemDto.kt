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
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEncryptionMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AddressDto
import org.taktik.icure.services.external.rest.v2.dto.embed.CalendarItemTagDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EncryptableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.FlowItemDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonIgnoreProperties(ignoreUnknown = true)
data class CalendarItemDto(
	override val id: String,
	override val rev: String? = null,
	override val created: Long? = null,
	override val modified: Long? = null,
	override val author: String? = null,
	override val responsible: String? = null,
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	override val tags: Set<CodeStubDto> = emptySet(),
	override val codes: Set<CodeStubDto> = emptySet(),
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val endOfLife: Long? = null,
	override val deletionDate: Long? = null,
	val title: String? = null,
	val calendarItemTypeId: String? = null,
	val masterCalendarItemId: String? = null,
	@Deprecated("Use crypedForeignKeys instead") val patientId: String? = null,
	val important: Boolean? = null,
	val homeVisit: Boolean? = null,
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val phoneNumber: String? = null,
	val placeId: String? = null,
	val address: AddressDto? = null,
	val addressText: String? = null,
	val startTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	val endTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	@Deprecated("Ignored by availabilities algorithm, will be replaced by another more descriptive field")
	val confirmationTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	@Deprecated("Ignored by availabilities algorithm, will be replaced by another more descriptive field")
	val cancellationTimestamp: Long? = null,
	@Deprecated("Ignored by availabilities algorithm, will be replaced by another more descriptive field")
	val confirmationId: String? = null,
	@Deprecated("Ignored by availabilities algorithm, use appropriate startTime and endTime")
	val duration: Long? = null,
	@Deprecated("Ignored by availabilities algorithm, use appropriate startTime and endTime")
	val allDay: Boolean? = null,
	val details: String? = null,
	val wasMigrated: Boolean? = null,
	val agendaId: String? = null,
	val resourceGroup: CodeStubDto? = null,
	val availabilitiesAssignmentStrategy: AvailabilitiesAssignmentStrategy? = null,
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val hcpId: String? = null,
	val recurrenceId: String? = null,
	val meetingTags: Set<CalendarItemTagDto> = emptySet(),
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val flowItem: FlowItemDto? = null,
	override val secretForeignKeys: Set<String> = emptySet(),
	override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	override val encryptedSelf: Base64StringDto? = null,
	override val securityMetadata: SecurityMetadataDto? = null,
) : StoredDocumentDto,
	ICureDocumentDto<String>,
	HasEncryptionMetadataDto,
	EncryptableDto {
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
