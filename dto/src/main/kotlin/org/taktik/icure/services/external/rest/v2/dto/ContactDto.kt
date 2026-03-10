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
import org.taktik.icure.services.external.rest.v2.dto.base.HasEncryptionMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEndOfLifeDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.base.ParticipantTypeDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AddressDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AnnotationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ContactParticipantDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EncryptableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ServiceDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SubContactDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
	description = """This entity is a root-level object. It represents a contact. It is serialized in JSON and saved in the underlying icure-contact CouchDB database.
The contact is the entity that records the medical information about the patient chronologically. A visit to the patient's house, a consultation at the practice, a phone call between the patient and the healthcare party or integrating lab reports into the medical file are examples of when a contact can be recorded.
A contact can occur with or without direct interaction between the patient and the healthcare party. For example, when a healthcare party encodes data received from laboratory's test result, this is done in the absence of a patient.
A contact groups together pieces of information collected during one single event, for one single patient and for one or more healthcare parties. Patient's complaints, the diagnosis of a new problem, a surgical procedure, etc. are collected inside a contact.
The main sub-element of the contact is the service. Each atomic piece of information collected during a contact is a service and is stored inside the services list of a contact.
""",
)
/**
 * This entity is a root-level object. It represents a contact. It is serialized in JSON and saved in the underlying
 * icure-contact CouchDB database.
 *
 * A contact is an entry in the day-to-day journal of the medical file of a patient. A contact happens between one
 * patient, one or several healthcare parties (with one healthcare party promoted as the responsible of the contact),
 * at one place during one (fairly short) period of time.
 * A contact contains a series of services (acts, observations, exchanges) performed on the patient. These services
 * can be linked to healthcare elements.
 *
 * A contact can occur with or without direct interaction between the patient and the healthcare party. For example,
 * when a healthcare party encodes data received from laboratory's test result, this is done in the absence of a patient.
 *
 * @property id The Id of the contact. We encourage using either a v4 UUID or a HL7 Id.
 * @property rev The revision of the contact in the database, used for conflict management / optimistic locking.
 * @property created The timestamp (unix epoch in ms) of creation of the contact, will be filled automatically if missing. Not enforced by the application server.
 * @property modified The date (unix epoch in ms) of the latest modification of the contact, will be filled automatically if missing. Not enforced by the application server.
 * @property author The id of the User that has created this contact, will be filled automatically if missing. Not enforced by the application server.
 * @property responsible The id of the HealthcareParty that is responsible for this contact, will be filled automatically if missing. Not enforced by the application server.
 * @property medicalLocationId The id of the medical location where the contact was recorded. Deprecated for use with Cardinal SDK.
 * @property tags Tags that qualify the contact as being member of a certain class.
 * @property codes Codes that identify or qualify this particular contact.
 * @property identifier The identifiers of the Contact.
 * @property endOfLife Soft delete (unix epoch in ms) timestamp of the object.
 * @property deletionDate Hard delete (unix epoch in ms) timestamp of the object.
 * @property groupId Separate contacts can be merged in one logical contact if they share the same groupId. When a contact must be split to selectively assign rights to healthcare parties, the split contacts all share the same groupId.
 * @property openingDate The date (YYYYMMDDhhmmss) of the start of the contact.
 * @property closingDate The date (YYYYMMDDhhmmss) marking the end of the contact.
 * @property descr Description of the contact.
 * @property location Location where the contact was recorded.
 * @property externalId An external (from another source) id with no guarantee or requirement for unicity. Deprecated for use with Cardinal SDK.
 * @property encounterType The type of encounter made for the contact.
 * @property encounterLocation The location where the encounter took place.
 * @property subContacts Set of all sub-contacts recorded during the given contact. Sub-contacts are used to link services embedded inside this contact to healthcare elements, healthcare approaches and/or forms.
 * @property services Set of all services provided to the patient during the contact.
 * @property participants The participants to the contact. The key is the type of participant, the value is the id of the participant data owner id. Deprecated: use [participantList] instead.
 * @property participantList The list of participants to the contact, with their type and data owner id.
 * @property healthcarePartyId Deprecated: use [responsible] instead.
 * @property modifiedContactId Deprecated: use [groupId] instead.
 * @property secretForeignKeys The secret patient key, encrypted in the patient document, in clear here.
 * @property cryptedForeignKeys The public patient key, encrypted here for separate Crypto Actors.
 * @property delegations The delegations giving access to connected healthcare information.
 * @property encryptionKeys The contact secret encryption key used to encrypt the secured properties (like services for example), encrypted for separate Crypto Actors.
 * @property encryptedSelf The encrypted fields of this contact.
 * @property securityMetadata The security metadata of this contact, tracking access control information.
 * @property notes Comments and notes recorded by a healthcare party about this contact.
 */
data class ContactDto(
	@param:Schema(description = "the Id of the contact. We encourage using either a v4 UUID or a HL7 Id.") override val id: String,
	@param:Schema(description = "the revision of the contact in the database, used for conflict management / optimistic locking.") override val rev: String? = null,
	@param:Schema(description = "The timestamp (unix epoch in ms) of creation of the contact, will be filled automatically if missing. Not enforced by the application server.") override val created: Long? = null,
	@param:Schema(description = "The date (unix epoch in ms) of the latest modification of the contact, will be filled automatically if missing. Not enforced by the application server.") override val modified: Long? = null,
	@param:Schema(description = "The id of the User that has created this contact, will be filled automatically if missing. Not enforced by the application server.") override val author: String? = null,
	@param:Schema(description = "The id of the HealthcareParty that is responsible for this contact, will be filled automatically if missing. Not enforced by the application server.") override val responsible: String? = null,
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "The id of the medical location where the contact was recorded.") override val medicalLocationId: String? = null,
	@param:Schema(description = "Tags that qualify the contact as being member of a certain class.") override val tags: Set<CodeStubDto> = emptySet(),
	@param:Schema(description = "Codes that identify or qualify this particular contact.") override val codes: Set<CodeStubDto> = emptySet(),
	@param:Schema(description = "The identifiers of the Contact") val identifier: List<IdentifierDto> = emptyList(),
	@param:Schema(description = "Soft delete (unix epoch in ms) timestamp of the object.") override val endOfLife: Long? = null,
	@param:Schema(description = "Hard delete (unix epoch in ms) timestamp of the object.") override val deletionDate: Long? = null,
	@param:Schema(
		description = "Separate contacts can be merged in one logical contact if they share the same groupId. When a contact must be split to selectively assign rights to healthcare parties, the split contacts all share the same groupId",
	) val groupId: String? = null,
	@param:Schema(description = "The date (YYYYMMDDhhmmss) of the start of the contact.") val openingDate: Long? = null,
	@param:Schema(description = "The date (YYYYMMDDhhmmss) marking the end of the contact.") val closingDate: Long? = null,
	@param:Schema(description = "Description of the contact") val descr: String? = null,
	@param:Schema(description = "Location where the contact was recorded.") val location: String? = null,
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "An external (from another source) id with no guarantee or requirement for unicity.") val externalId: String? = null,
	@param:Schema(description = "The type of encounter made for the contact") val encounterType: CodeStubDto? = null,
	@param:Schema(description = "The location where the encounter took place") val encounterLocation: AddressDto? = null,
	@param:Schema(
		description = "Set of all sub-contacts recorded during the given contact. Sub-contacts are used to link services embedded inside this contact to healthcare elements, healthcare approaches and/or forms.",
	) val subContacts: Set<SubContactDto> = emptySet(),
	@param:Schema(description = "Set of all services provided to the patient during the contact.") val services: Set<ServiceDto> = emptySet(),
	@param:Schema(description = "The participants to the contact. The key is the type of participant, the value is the id of the participant data owner id")
	@Deprecated("Use participantList", replaceWith = ReplaceWith("participantList"))
	val participants: Map<ParticipantTypeDto, String> = emptyMap(),
	@param:Schema(description = "The list of participants to the contact, with their type and data owner id.")
	@param:JsonInclude(JsonInclude.Include.NON_EMPTY) val participantList: List<ContactParticipantDto> = emptyList(),
	@Deprecated("Use responsible") val healthcarePartyId: String? = null,
	@Deprecated("Use groupId") val modifiedContactId: String? = null,
	@param:Schema(description = "The secret patient key, encrypted in the patient document, in clear here.") override val secretForeignKeys: Set<String> = emptySet(),
	@param:Schema(description = "The public patient key, encrypted here for separate Crypto Actors.") override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	@param:Schema(description = "The delegations giving access to connected healthcare information.") override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	@param:Schema(description = "The contact secret encryption key used to encrypt the secured properties (like services for example), encrypted for separate Crypto Actors.") override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	@param:Schema(description = "The encrypted fields of this contact.") override val encryptedSelf: Base64StringDto? = null,
	@param:Schema(description = "The security metadata of this contact, tracking access control information.") override val securityMetadata: SecurityMetadataDto? = null,
	@param:Schema(description = "Comments - Notes recorded by a HCP about this contact") val notes: List<AnnotationDto> = emptyList(),
) : StoredDocumentDto,
	ICureDocumentDto<String>,
	HasEncryptionMetadataDto,
	EncryptableDto,
	HasEndOfLifeDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
