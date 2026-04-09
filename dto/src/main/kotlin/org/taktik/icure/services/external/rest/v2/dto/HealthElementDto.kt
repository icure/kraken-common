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

import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.entities.RawJson
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEncryptionMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEndOfLifeDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AnnotationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.CareTeamMemberDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EncryptableDto
import org.taktik.icure.services.external.rest.v2.dto.base.ExtendableRootDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EpisodeDto
import org.taktik.icure.services.external.rest.v2.dto.embed.LateralityDto
import org.taktik.icure.services.external.rest.v2.dto.embed.PlanOfActionDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
	description = """This entity is a root level object. It represents a healthcare element. It is serialized in JSON and saved in the underlying CouchDB database.""",
)
@JsonFilter("healthElementFilter")
/**
 * Represents a healthcare element. A healthcare element is a patient-centric representation of a healthcare problem
 * that evolves over time in its name/label and characteristics. It is a central element in the organisation of the
 * electronic health record, used to filter and link medical data in a meaningful way.
 */
data class HealthElementDto(
	/** The Id of the healthcare element. We encourage using either a v4 UUID or a HL7 Id. */
	@param:Schema(description = "The Id of the healthcare element. We encourage using either a v4 UUID or a HL7 Id.") override val id: String,
	/** The identifiers of the healthcare element. */
	val identifiers: List<IdentifierDto> = emptyList(),
	/** The revision of the healthcare element in the database, used for conflict management / optimistic locking. */
	@param:Schema(description = "The revision of the patient in the database, used for conflict management / optimistic locking.")
	override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation. */
	override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification. */
	override val modified: Long? = null,
	/** The id of the User that created this healthcare element. */
	override val author: String? = null,
	/** The id of the HealthcareParty that is responsible for this healthcare element. */
	override val responsible: String? = null,
	/** The id of the medical location where this healthcare element was created. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** Tags that qualify the healthcare element as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this particular healthcare element. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	override val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** The logical id of the healthcare element, used to link together different versions of the same healthcare element. */
	@param:Schema(description = "The logical id of the healthcare element, used to link together different versions of the same healthcare element. We encourage using either a v4 UUID or a HL7 Id.")
	val healthElementId: String? = null,
	// Usually one of the following is used (either valueDate or openingDate and closingDate)
	/** The date (unix epoch in ms) when the healthcare element is noted to have started and also closes on the same date. */
	@param:Schema(description = "The date (unix epoch in ms) when the healthcare element is noted to have started and also closes on the same date")
	val valueDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	/** The date (unix epoch in ms) of the start of the healthcare element. */
	@param:Schema(description = "The date (unix epoch in ms) of the start of the healthcare element.") val openingDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	/** The date (unix epoch in ms) marking the end of the healthcare element. */
	@param:Schema(description = "The date (unix epoch in ms) marking the end of the healthcare element.") val closingDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	/** Description of the healthcare element. */
	@param:Schema(description = "Description of the healthcare element.") val descr: String? = null,
	/** A text note (can be confidential, encrypted by default). */
	@param:Schema(description = "A text note (can be confidential, encrypted by default).") val note: String? = null,
	/** Localized text annotations for the healthcare element. */
	val notes: List<AnnotationDto> = emptyList(),
	/** Whether the healthcare element is relevant or not. */
	@param:Schema(description = "If the healthcare element is relevant or not (Set relevant by default).", defaultValue = "true") val relevant: Boolean = true,
	/** Id of the opening contact when the healthcare element was created. */
	@param:Schema(description = "Id of the opening contact when the healthcare element was created.") val idOpeningContact: String? = null,
	/** Id of the closing contact for the healthcare element. */
	@param:Schema(description = "Id of the closing contact for the healthcare element.") val idClosingContact: String? = null,
	/** Id of the service when a service is used to create a healthcare element. */
	@param:Schema(description = "Id of the service when a service is used to create a healthcare element.") val idService: String? = null, // When a service is used to create the healthElement
	/** Bit field representing active/inactive, relevant/irrelevant, present/absent states. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "bit 0: active/inactive, bit 1: relevant/irrelevant, bit 2 : present/absent, ex: 0 = active,relevant and present", defaultValue = "0")
	val status: Int = 0, // bit 0: active/inactive, bit 1: relevant/irrelevant, bit 2 : present/absent, ex: 0 = active,relevant and present
	/** Left or right dominance/preference. */
	@param:Schema(description = "Left or Right dominance/preference.") val laterality: LateralityDto? = null,
	/** List of healthcare approaches. */
	@param:Schema(description = "List of healthcare approaches.") val plansOfAction: List<PlanOfActionDto> = emptyList(),
	/** List of episodes of occurrences of the healthcare element. */
	@param:Schema(description = "List of episodes of occurrences of the healthcare element.") val episodes: List<EpisodeDto> = emptyList(),
	/** List of care team members assigned for the healthcare element. */
	@param:Schema(description = "List of care team members assigned for the healthcare element.") val careTeam: List<CareTeamMemberDto> = emptyList(),
	/** The secret patient key, encrypted in the patient's own AES key. */
	override val secretForeignKeys: Set<String> = emptySet(),
	/** The patient id encrypted in the delegates' AES keys. */
	override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The delegations giving access to connected healthcare information. */
	override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The keys used to encrypt this entity when the entity is stored encrypted. */
	override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The base64-encoded encrypted fields of this entity. */
	override val encryptedSelf: Base64StringDto? = null,
	/** The security metadata of the entity. */
	override val securityMetadata: SecurityMetadataDto? = null,
	override val extensions: RawJson.JsonObject? = null,
	override val extensionsVersion: Int? = null,
) : StoredDocumentDto,
	ICureDocumentDto<String>,
	HasEncryptionMetadataDto,
	EncryptableDto,
	HasEndOfLifeDto,
	ExtendableRootDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
