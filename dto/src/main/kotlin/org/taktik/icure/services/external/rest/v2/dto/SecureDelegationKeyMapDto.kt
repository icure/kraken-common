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
import io.swagger.v3.oas.annotations.extensions.Extension
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.CryptoActorDto
import org.taktik.icure.services.external.rest.v2.dto.base.EncryptableDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.base.PersonDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AddressDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AnnotationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DeactivationReasonDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EmploymentInfoDto
import org.taktik.icure.services.external.rest.v2.dto.embed.FinancialInstitutionInformationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.GenderDto
import org.taktik.icure.services.external.rest.v2.dto.embed.InsurabilityDto
import org.taktik.icure.services.external.rest.v2.dto.embed.MedicalHouseContractDto
import org.taktik.icure.services.external.rest.v2.dto.embed.PartnershipDto
import org.taktik.icure.services.external.rest.v2.dto.embed.PatientHealthCarePartyDto
import org.taktik.icure.services.external.rest.v2.dto.embed.PersonNameDto
import org.taktik.icure.services.external.rest.v2.dto.embed.PersonalStatusDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SchoolingInfoDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SecureDelegationKeyMapDto(
	override val id: String,
	override val rev: String? = null,
	/**
	 * The secure delegation key this map refers to.
	 */
	val delegationKey: String,
	/**
	 * The delegator of the secure delegation key this map refers to, if the delegator is anonymous in the delegation,
	 * and if not encrypted.
	 * On the server side this value should always be encrypted.
	 */
	val delegator: String?,
	/**
	 * The delegate of the secure delegation key this map refers to, if the delegate is anonymous in the delegation,
	 * and if not encrypted.
	 * On the server side this value should always be encrypted.
	 */
	val delegate: String?,
	override val secretForeignKeys: Set<String> = emptySet(),
	override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	override val encryptedSelf: String? = null,
	override val securityMetadata: SecurityMetadataDto? = null,
	override val deletionDate: Long? = null,
) : StoredDocumentDto, EncryptableDto {
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
