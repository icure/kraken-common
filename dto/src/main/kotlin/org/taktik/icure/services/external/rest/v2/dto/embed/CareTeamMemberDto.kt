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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifiableDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a member of a care team involved in a patient's care, linking a healthcare party with their role.
 */
data class CareTeamMemberDto(
	/** The unique identifier of this care team member. */
	override val id: String,
	/** The type of care team member (physician, specialist, or other). */
	val careTeamMemberType: CareTeamMemberTypeDto? = null,
	/** The identifier of the associated healthcare party. */
	val healthcarePartyId: String? = null,
	/** A code describing the quality or qualification of this care team member. */
	val quality: CodeStubDto? = null,
	/** The base64-encoded encrypted content of this care team member. */
	override val encryptedSelf: Base64StringDto? = null,
) : EncryptableDto,
	Serializable,
	IdentifiableDto<String> {
	companion object : DynamicInitializer<CareTeamMemberDto>

	fun merge(other: CareTeamMemberDto) = CareTeamMemberDto(args = this.solveConflictsWith(other))

	fun solveConflictsWith(other: CareTeamMemberDto) = super.solveConflictsWith(other) +
		mapOf(
			"id" to (this.id),
			"careTeamMemberType" to (this.careTeamMemberType ?: other.careTeamMemberType),
			"healthcarePartyId" to (this.healthcarePartyId ?: other.healthcarePartyId),
			"quality" to (this.quality ?: other.quality),
		)
}
