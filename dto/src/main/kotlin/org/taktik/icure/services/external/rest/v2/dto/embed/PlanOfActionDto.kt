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
package org.taktik.icure.services.external.rest.v2.dto.embed

/**
 * Created by aduchate on 09/07/13, 16:30
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.NamedDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PlanOfActionDto(
	override val id: String,
	override val created: Long? = null,
	override val modified: Long? = null,
	override val author: String? = null,
	override val responsible: String? = null,
	override val medicalLocationId: String? = null,
	override val tags: Set<CodeStubDto> = emptySet(),
	override val codes: Set<CodeStubDto> = emptySet(),
	override val endOfLife: Long? = null,
	// Usually one of the following is used (either valueDate or openingDate and closingDate)
	@get:Schema(description = "The id of the hcp who prescribed this healthcare approach") val prescriberId: String? = null, // healthcarePartyId
	@get:Schema(
		description = "The date (unix epoch in ms) when the healthcare approach is noted to have started and also closes on the same date",
	) val valueDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	@get:Schema(description = "The date (unix epoch in ms) of the start of the healthcare approach.") val openingDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	@get:Schema(description = "The date (unix epoch in ms) marking the end of the healthcare approach.") val closingDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	@get:Schema(description = "The date (unix epoch in ms) when the healthcare approach has to be carried out.") val deadlineDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20140101235960.
	@get:Schema(description = "The name of the healthcare approach.") override val name: String? = null,
	@get:Schema(description = "Description of the healthcare approach.") val descr: String? = null,
	@get:Schema(description = "Note about the healthcare approach.") val note: String? = null,
	@get:Schema(description = "Id of the opening contact when the healthcare approach was created.") val idOpeningContact: String? = null,
	@get:Schema(description = "Id of the closing contact for the healthcare approach.") val idClosingContact: String? = null,
	@get:Schema(
		description = "bit 0: active/inactive, bit 1: relevant/irrelevant, bit 2 : present/absent, ex: 0 = active,relevant and present",
		defaultValue = "0",
	) val status: Int = 0, // bit 0: active/inactive, bit 1: relevant/irrelevant, bit 2 : present/absent, ex: 0 = active,relevant and present
	@get:Deprecated("Use services linked to this healthcare approach") val documentIds: Set<String> = emptySet(),
	@get:Deprecated(
		"Use services (one per care) linked to this healthcare approach",
	) @Schema(description = "The number of individual cares already performed in the course of this healthcare approach") val numberOfCares: Int? = null,
	@get:Schema(description = "Members of the careteam involved in this approach") val careTeamMemberships: List<CareTeamMembershipDto?> =
		emptyList(),
	@get:Deprecated("Use status") @Schema(defaultValue = "true")val relevant: Boolean = true,
	override val encryptedSelf: Base64StringDto? = null,
) : EncryptableDto,
	ICureDocumentDto<String>,
	NamedDto
