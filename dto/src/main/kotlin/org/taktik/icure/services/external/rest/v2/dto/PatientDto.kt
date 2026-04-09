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
import org.taktik.icure.AlwaysDecrypted
import org.taktik.icure.entities.RawJson
import org.taktik.icure.SdkNonNullable
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.CryptoActorDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEncryptionMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.base.PersonDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AddressDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AnnotationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EmploymentInfoDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EncryptableDto
import org.taktik.icure.services.external.rest.v2.dto.base.ExtendableRootDto
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
import org.taktik.icure.services.external.rest.v2.dto.specializations.AesExchangeKeyEncryptionKeypairIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.AesExchangeKeyEntryKeyStringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.HexStringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.SpkiHexStringDto

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
	description = """This entity is a root level object. It represents a patient It is serialized in JSON and saved in the underlying icure-patient CouchDB database.""",
	extensions = [
		Extension(
			name = "is-deprecated",
			properties = [
				ExtensionProperty(
					name = "fatherBirthCountry",
					value = "true",
				), ExtensionProperty(
					name = "birthCountry",
					value = "true",
				), ExtensionProperty(
					name = "nativeCountry",
					value = "true",
				), ExtensionProperty(name = "socialStatus", value = "true"), ExtensionProperty(name = "mainSourceOfIncome", value = "true"),
			],
		),
	],
)
/**
 * Represents a patient in the iCure platform. A patient is a person who receives healthcare services.
 * This entity stores personal, administrative, and medical information about the patient, and supports
 * end-to-end encryption of sensitive data.
 */
data class PatientDto(
	/** The Id of the patient. We encourage using either a v4 UUID or a HL7 Id. */
	@param:Schema(description = "the Id of the patient. We encourage using either a v4 UUID or a HL7 Id.") override val id: String,
	/** The patient's identifiers, used by the client to uniquely identify the patient. */
	val identifier: List<IdentifierDto> = emptyList(),
	/** The revision of the patient in the database, used for conflict management / optimistic locking. */
	@param:Schema(description = "the revision of the patient in the database, used for conflict management / optimistic locking.") override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation. */
	override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification. */
	override val modified: Long? = null,
	/** The id of the User that created this patient. */
	override val author: String? = null,
	/** The id of the HealthcareParty that is responsible for this patient. */
	override val responsible: String? = null,
	/** Tags that qualify the patient as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this particular patient. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** The firstname (name) of the patient. */
	@param:Schema(description = "the firstname (name) of the patient.") override val firstName: String? = null,
	/** The lastname (surname) of the patient. */
	@param:Schema(description = "the lastname (surname) of the patient. This is the official lastname that should be used for official administrative purposes.")
	override val lastName: String? = null, // Is usually either maidenName or spouseName,
	/** The list of all names of the patient, ordered by preference of use. */
	@param:Schema(description = "the list of all names of the patient, also containing the official full name information. Ordered by preference of use. First element is therefore the official name used for the patient in the application")
	override val names: List<PersonNameDto> = emptyList(),
	/** The name of the company this patient is member of. */
	@param:Schema(description = "the name of the company this patient is member of.") override val companyName: String? = null,
	/** The list of languages spoken by the patient ordered by fluency (alpha-2 code). */
	@param:Schema(description = "the list of languages spoken by the patient ordered by fluency (alpha-2 code http://www.loc.gov/standards/iso639-2/ascii_8bits.html).")
	override val languages: List<String> = emptyList(), // alpha-2 code http://www.loc.gov/standards/iso639-2/ascii_8bits.html,
	/** The list of addresses (with address type). */
	@param:Schema(description = "the list of addresses (with address type).") override val addresses: List<AddressDto> = emptyList(),
	/** Mr., Ms., Pr., Dr. ... */
	@param:Schema(description = "Mr., Ms., Pr., Dr. ...") override val civility: String? = null,
	@param:Schema(
		description = "the gender of the patient: male, female, indeterminate, changed, changedToMale, changedToFemale, unknown",
		defaultValue = "GenderDto.unknown",
	/** The gender of the patient. */
	) override val gender: GenderDto? = GenderDto.unknown,
	@param:Schema(
		description = "the birth sex of the patient: male, female, indeterminate, unknown",
		defaultValue = "GenderDto.unknown",
	/** The birth sex of the patient. */
	) val birthSex: GenderDto? = GenderDto.unknown,
	/** The id of the patient this patient has been merged with. */
	@param:Schema(description = "The id of the patient this patient has been merged with.") val mergeToPatientId: String? = null,
	/** The ids of the patients that have been merged inside this patient. */
	@param:Schema(description = "The ids of the patients that have been merged inside this patient.") val mergedIds: Set<String> = emptySet(),
	/** An alias of the person, nickname, ... */
	@param:Schema(description = "An alias of the person, nickname, ...") val alias: String? = null,
	/** Whether the patient is active. */
	@param:Schema(description = "Is the patient active (boolean).", defaultValue = "true") val active: Boolean = true,
	/** When not active, the reason for deactivation. */
	@param:Schema(description = "When not active, the reason for deactivation.", defaultValue = "\"none\"") val deactivationReason: String = "none",
	/** Deactivation date of the patient. */
	@param:Schema(description = "Deactivation date of the patient") val deactivationDate: Int? = null,
	/** Social security inscription number. */
	@param:Schema(description = "Social security inscription number.") val ssin: String? = null,
	@param:Schema(
		description = "Lastname at birth (can be different of the current name), depending on the country, must be used to design the patient .",
	/** Lastname at birth (can be different from the current name). */
	) val maidenName: String? = null, // Never changes (nom de jeune fille),
	@param:Schema(
		description = "Lastname of the spouse for a married woman, depending on the country, can be used to design the patient.",
	/** Lastname of the spouse for a married woman. */
	) val spouseName: String? = null, // Name of the spouse after marriage,
	/** Lastname of the partner. */
	@param:Schema(description = "Lastname of the partner, should not be used to design the patient.") val partnerName: String? = null, // Name of the partner, sometimes equal to spouseName,
	@param:Schema(
		description = "any of `single`, `in_couple`, `married`, `separated`, `divorced`, `divorcing`, `widowed`, `widower`, `complicated`, `unknown`, `contract`, `other`.",
		defaultValue = "PersonalStatusDto.unknown",
	/** The personal/marital status of the patient. */
	) val personalStatus: PersonalStatusDto? = PersonalStatusDto.unknown,
	@param:Schema(
		description = "The birthdate encoded as a fuzzy date on 8 positions (YYYYMMDD) MM and/or DD can be set to 00 if unknown (19740000 is a valid date).",
	/** The birthdate encoded as a fuzzy date on 8 positions (YYYYMMDD). */
	) val dateOfBirth: Int? = null, // YYYYMMDD if unknown, 00, ex:20010000 or,
	@param:Schema(
		description = "The date of death encoded as a fuzzy date on 8 positions (YYYYMMDD) MM and/or DD can be set to 00 if unknown (19740000 is a valid date).",
	/** The date of death encoded as a fuzzy date on 8 positions (YYYYMMDD). */
	) val dateOfDeath: Int? = null, // YYYYMMDD if unknown, 00, ex:20010000 or,
	/** Timestamp of the latest validation of the eID of the person. */
	@param:Schema(description = "Timestamp of the latest validation of the eID of the person..") val timestampOfLatestEidReading: Long? = null,
	/** The place of birth. */
	@param:Schema(description = "The place of birth.") val placeOfBirth: String? = null,
	/** The place of death. */
	@param:Schema(description = "The place of death.") val placeOfDeath: String? = null,
	/** Whether the patient is deceased. */
	@param:Schema(description = "Is the patient deceased.") val deceased: Boolean? = null,
	/** The level of education (college degree, undergraduate, phd). */
	@param:Schema(description = "The level of education (college degree, undergraduate, phd).") val education: String? = null,
	/** The current professional activity. */
	@param:Schema(description = "The current professional activity.") val profession: String? = null,
	/** Localized text notes (can be confidential). */
	@param:Schema(description = "Localized text notes (can be confidential).") val notes: List<AnnotationDto> = emptyList(),
	/** A text note (can be confidential, encrypted by default). */
	@param:Schema(description = "A text note (can be confidential, encrypted by default).", deprecated = true) val note: String? = null,
	/** An administrative note, not confidential. */
	@param:Schema(description = "An administrative note, not confidential.", deprecated = true) val administrativeNote: String? = null,
	/** The nationality of the patient. */
	@param:Schema(description = "The nationality of the patient.") val nationality: String? = null,
	/** The race of the patient. */
	@param:Schema(description = "The race of the patient.") val race: String? = null,
	/** The ethnicity of the patient. */
	@param:Schema(description = "The ethnicity of the patient.") val ethnicity: String? = null,
	/** The id of the user that usually handles this patient. */
	@Deprecated("Discouraged, use custom property if you really want them") @param:Schema(description = "The id of the user that usually handles this patient.") val preferredUserId: String? = null,
	/** A picture usually saved in JPEG format. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "A picture usually saved in JPEG format.", type = "string", format = "byte") val picture: ByteArray? = null,
	/** An external (from another source) id with no guarantee of unicity. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "An external (from another source) id with no guarantee or requirement for unicity .")
	val externalId: String? = null, // No guarantee of unicity
	/** List of insurance coverages. */
	@param:Schema(description = "List of insurance coverages (of class Insurability, see below).")
	val insurabilities: List<InsurabilityDto> = emptyList(),
	/** List of partners, or persons of contact. */
	@param:Schema(description = "List of partners, or persons of contact (of class Partnership, see below)")
	val partnerships: List<PartnershipDto> = emptyList(),
	/** Links between this patient and healthcare parties. */
	@param:Schema(description = "Links (usually for therapeutic reasons) between this patient and healthcare parties (of class PatientHealthcareParty)")
	val patientHealthCareParties: List<PatientHealthCarePartyDto> = emptyList(),
	/** Financial information used to reimburse the patient. */
	@param:Schema(description = "Financial information (Bank, bank account) used to reimburse the patient.")
	val financialInstitutionInformation: List<FinancialInstitutionInformationDto> = emptyList(),
	/** Contracts between the patient and the healthcare entity. */
	@param:Schema(description = "Contracts between the patient and the healthcare entity")
	val medicalHouseContracts: List<MedicalHouseContractDto> = emptyList(),
	/** Codified list of professions exercised by this patient. */
	@param:Schema(description = "Codified list of professions exercised by this patient.") val patientProfessions: List<CodeStubDto> = emptyList(),
	/** Extra parameters. */
	@param:Schema(description = "Extra parameters") val parameters: Map<String, List<String>> = emptyMap(),
	/** Extra properties. */
	@param:Schema(description = "Extra properties") val properties: Set<PropertyStubDto> = emptySet(),
	/** For each couple of HcParties, the AES exchange key. */
	override val hcPartyKeys: Map<String, List<HexStringDto>> = emptyMap(),
	/** Extra AES exchange keys, indexed by the owner of the pair and target data owner id. */
	override val aesExchangeKeys: Map<AesExchangeKeyEntryKeyStringDto, Map<String, Map<AesExchangeKeyEncryptionKeypairIdentifierDto, HexStringDto>>> = emptyMap(),
	/** Keys used to transfer ownership of encrypted data between key pairs. */
	override val transferKeys: Map<AesExchangeKeyEncryptionKeypairIdentifierDto, Map<AesExchangeKeyEncryptionKeypairIdentifierDto, HexStringDto>> = emptyMap(),
	/** Shamir partitions of the private key. */
	override val privateKeyShamirPartitions: Map<String, HexStringDto> = emptyMap(),
	/** The public key of this patient, used to encrypt data for this patient. */
	override val publicKey: SpkiHexStringDto? = null,
	/** Public keys for OAEP with SHA-256 encryption. */
	override val publicKeysForOaepWithSha256: Set<SpkiHexStringDto> = emptySet(),
	/** The secret patient key, encrypted in the patient's own AES key. */
	override val secretForeignKeys: Set<String> = emptySet(),
	/** The patient id encrypted in the delegates' AES keys. */
	override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The delegations giving access to connected healthcare information. */
	override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The keys used to encrypt this entity when stored encrypted. */
	override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The base64-encoded encrypted fields of this entity. */
	override val encryptedSelf: Base64StringDto? = null,
	/** The security metadata of the entity. */
	override val securityMetadata: SecurityMetadataDto? = null,
	/** Properties related to crypto actor functionality. */
	@SdkNonNullable @AlwaysDecrypted @param:JsonInclude(JsonInclude.Include.NON_NULL) override val cryptoActorProperties: Set<PropertyStubDto>? = null,
	/** The id of the medical location where this patient was created. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** Set of patient ids that are not duplicates of this patient. */
	@Deprecated("Do not use") val nonDuplicateIds: Set<String> = emptySet(),
	/** Set of encrypted administrative documents. */
	@Deprecated("Do not use") val encryptedAdministrativesDocuments: Set<String> = emptySet(),
	/** A comment on the patient (deprecated, use note or administrativeNote). */
	@Deprecated("Use note or administrativeNote") val comment: String? = null,
	/** A warning on the patient (deprecated, use note or administrativeNote). */
	@Deprecated("Use note or administrativeNote") val warning: String? = null,
	/** The father's birth country (deprecated, use properties instead). */
	@Deprecated("Use properties instead") val fatherBirthCountry: CodeStubDto? = null, // Deprecated won't work on $ref, because the serialisation gets rid of everything that is not $ref (in conformance with the spec)
	/** The patient's birth country (deprecated, use properties instead). */
	@Deprecated("Use properties instead") val birthCountry: CodeStubDto? = null, // Deprecated won't work on $ref, because the serialisation gets rid of everything that is not $ref (in conformance with the spec)
	/** The patient's native country (deprecated, use properties instead). */
	@Deprecated("Use properties instead") val nativeCountry: CodeStubDto? = null, // Deprecated won't work on $ref, because the serialisation gets rid of everything that is not $ref (in conformance with the spec)
	/** The social status of the patient (deprecated, use properties instead). */
	@Deprecated("Use properties instead") val socialStatus: CodeStubDto? = null, // Deprecated won't work on $ref, because the serialisation gets rid of everything that is not $ref (in conformance with the spec)
	/** The main source of income (deprecated, use properties instead). */
	@Deprecated("Use properties instead") val mainSourceOfIncome: CodeStubDto? = null, // Deprecated won't work on $ref, because the serialisation gets rid of everything that is not $ref (in conformance with the spec)
	/** Schooling information (deprecated, use properties instead). */
	@Deprecated("Use properties instead") val schoolingInfos: List<SchoolingInfoDto> = emptyList(),
	/** Employment information (deprecated, use properties instead). */
	@Deprecated("Use properties instead") val employementInfos: List<EmploymentInfoDto> = emptyList(),
	/** Always null for patients. */
	override val parentId: Nothing? = null,
	override val extensions: RawJson.JsonObject? = null,
	override val extensionsVersion: Int? = null,
) : StoredDocumentDto,
	ICureDocumentDto<String>,
	PersonDto,
	HasEncryptionMetadataDto,
	EncryptableDto,
	CryptoActorDto,
	ExtendableRootDto
	{
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
