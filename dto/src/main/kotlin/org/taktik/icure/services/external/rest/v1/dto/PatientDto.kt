/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.extensions.Extension
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v1.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v1.dto.base.CryptoActorDto
import org.taktik.icure.services.external.rest.v1.dto.base.HasEncryptionMetadataDto
import org.taktik.icure.services.external.rest.v1.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v1.dto.base.PersonDto
import org.taktik.icure.services.external.rest.v1.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.embed.AddressDto
import org.taktik.icure.services.external.rest.v1.dto.embed.AnnotationDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DeactivationReasonDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v1.dto.embed.EmploymentInfoDto
import org.taktik.icure.services.external.rest.v1.dto.embed.EncryptableDto
import org.taktik.icure.services.external.rest.v1.dto.embed.FinancialInstitutionInformationDto
import org.taktik.icure.services.external.rest.v1.dto.embed.GenderDto
import org.taktik.icure.services.external.rest.v1.dto.embed.InsurabilityDto
import org.taktik.icure.services.external.rest.v1.dto.embed.MedicalHouseContractDto
import org.taktik.icure.services.external.rest.v1.dto.embed.PartnershipDto
import org.taktik.icure.services.external.rest.v1.dto.embed.PatientHealthCarePartyDto
import org.taktik.icure.services.external.rest.v1.dto.embed.PersonNameDto
import org.taktik.icure.services.external.rest.v1.dto.embed.PersonalStatusDto
import org.taktik.icure.services.external.rest.v1.dto.embed.SchoolingInfoDto
import org.taktik.icure.services.external.rest.v1.dto.embed.SecurityMetadataDto

@JsonInclude(JsonInclude.Include.NON_NULL)
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
data class PatientDto(
	@param:Schema(description = "the Id of the patient. We encourage using either a v4 UUID or a HL7 Id.") override val id: String,
	@param:Schema(description = "the revision of the patient in the database, used for conflict management / optimistic locking.") override val rev: String? = null,
	val identifier: List<IdentifierDto> = emptyList(),
	override val created: Long? = null,
	override val modified: Long? = null,
	override val author: String? = null,
	override val responsible: String? = null,
	override val tags: Set<CodeStubDto> = emptySet(),
	override val codes: Set<CodeStubDto> = emptySet(),
	override val endOfLife: Long? = null,
	override val deletionDate: Long? = null,
	@param:Schema(description = "the firstname (name) of the patient.") override val firstName: String? = null,
	@param:Schema(
		description = "the lastname (surname) of the patient. This is the official lastname that should be used for official administrative purposes.",
	) override val lastName: String? = null, // Is usually either maidenName or spouseName,
	@param:Schema(
		description = "the list of all names of the patient, also containing the official full name information. Ordered by preference of use. First element is therefore the official name used for the patient in the application",
	) override val names: List<PersonNameDto> = emptyList(),
	@param:Schema(description = "the name of the company this patient is member of.") override val companyName: String? = null,
	@param:Schema(
		description = "the list of languages spoken by the patient ordered by fluency (alpha-2 code http://www.loc.gov/standards/iso639-2/ascii_8bits.html).",
	) override val languages: List<String> = emptyList(), // alpha-2 code http://www.loc.gov/standards/iso639-2/ascii_8bits.html,
	@param:Schema(description = "the list of addresses (with address type).") override val addresses: List<AddressDto> = emptyList(),
	@param:Schema(description = "Mr., Ms., Pr., Dr. ...") override val civility: String? = null,
	@param:Schema(
		description = "the gender of the patient: male, female, indeterminate, changed, changedToMale, changedToFemale, unknown",
	) override val gender: GenderDto? = GenderDto.unknown,
	@param:Schema(description = "the birth sex of the patient: male, female, indeterminate, unknown") val birthSex: GenderDto? = GenderDto.unknown,
	@param:Schema(description = "The id of the patient this patient has been merged with.") val mergeToPatientId: String? = null,
	@param:Schema(description = "The ids of the patients that have been merged inside this patient.") val mergedIds: Set<String> = emptySet(),
	@param:Schema(description = "An alias of the person, nickname, ...") val alias: String? = null,
	@param:Schema(description = "Is the patient active (boolean).", defaultValue = "true") val active: Boolean = true,
	@param:Schema(description = "When not active, the reason for deactivation.", defaultValue = "\"none\"") val deactivationReason: String = "none",
	@param:Schema(description = "Deactivation date of the patient") val deactivationDate: Int? = null,
	@param:Schema(description = "Social security inscription number.") val ssin: String? = null,
	@param:Schema(
		description = "Lastname at birth (can be different of the current name), depending on the country, must be used to design the patient .",
	) val maidenName: String? = null, // Never changes (nom de jeune fille),
	@param:Schema(
		description = "Lastname of the spouse for a married woman, depending on the country, can be used to design the patient.",
	) val spouseName: String? = null, // Name of the spouse after marriage,
	@param:Schema(description = "Lastname of the partner, should not be used to design the patient.") val partnerName: String? = null, // Name of the partner, sometimes equal to spouseName,
	@param:Schema(
		description = "any of `single`, `in_couple`, `married`, `separated`, `divorced`, `divorcing`, `widowed`, `widower`, `complicated`, `unknown`, `contract`, `other`.",
	) val personalStatus: PersonalStatusDto? = PersonalStatusDto.unknown,
	@param:Schema(
		description = "The birthdate encoded as a fuzzy date on 8 positions (YYYYMMDD) MM and/or DD can be set to 00 if unknown (19740000 is a valid date).",
	) val dateOfBirth: Int? = null, // YYYYMMDD if unknown, 00, ex:20010000 or,
	@param:Schema(
		description = "The date of death encoded as a fuzzy date on 8 positions (YYYYMMDD) MM and/or DD can be set to 00 if unknown (19740000 is a valid date).",
	) val dateOfDeath: Int? = null, // YYYYMMDD if unknown, 00, ex:20010000 or,
	@param:Schema(description = "Timestamp of the latest validation of the eID of the person..") val timestampOfLatestEidReading: Long? = null,
	@param:Schema(description = "The place of birth.") val placeOfBirth: String? = null,
	@param:Schema(description = "The place of death.") val placeOfDeath: String? = null,
	@param:Schema(description = "Is the patient deceased.") val deceased: Boolean? = null,
	@param:Schema(description = "The level of education (college degree, undergraduate, phd).") val education: String? = null,
	@param:Schema(description = "The current professional activity.") val profession: String? = null,
	@param:Schema(description = "Localized text notes (can be confidential).") val notes: List<AnnotationDto> = emptyList(),
	@param:Schema(description = "A text note (can be confidential, encrypted by default).", deprecated = true) val note: String? = null,
	@param:Schema(description = "An administrative note, not confidential.", deprecated = true) val administrativeNote: String? = null,
	@param:Schema(description = "The nationality of the patient.") val nationality: String? = null,
	@param:Schema(description = "The race of the patient.") val race: String? = null,
	@param:Schema(description = "The ethnicity of the patient.") val ethnicity: String? = null,
	@param:Schema(description = "The id of the user that usually handles this patient.") val preferredUserId: String? = null,
	@param:Schema(description = "A picture usually saved in JPEG format.", type = "string", format = "byte") val picture: ByteArray? = null,
	@param:Schema(
		description = "An external (from another source) id with no guarantee or requirement for unicity .",
	) val externalId: String? = null, // No guarantee of unicity
	@param:Schema(description = "List of insurance coverages (of class Insurability, see below).") val insurabilities: List<InsurabilityDto> =
		emptyList(),
	@param:Schema(
		description = "List of partners, or persons of contact (of class Partnership, see below).",
	) val partnerships: List<PartnershipDto> =
		emptyList(),
	@param:Schema(
		description = "Links (usually for therapeutic reasons) between this patient and healthcare parties (of class PatientHealthcareParty).",
	) val patientHealthCareParties: List<PatientHealthCarePartyDto> = emptyList(),
	@param:Schema(description = "Financial information (Bank, bank account) used to reimburse the patient.") val financialInstitutionInformation: List<FinancialInstitutionInformationDto> = emptyList(),
	@param:Schema(
		description = "Contracts between the patient and the healthcare entity.",
	) val medicalHouseContracts: List<MedicalHouseContractDto> =
		emptyList(),
	@param:Schema(description = "Codified list of professions exercised by this patient.") val patientProfessions: List<CodeStubDto> = emptyList(),
	@param:Schema(description = "Extra parameters") val parameters: Map<String, List<String>> = emptyMap(),
	@param:Schema(description = "Extra properties") val properties: Set<PropertyStubDto> = emptySet(),
	override val hcPartyKeys: Map<String, List<String>> = emptyMap(),
	override val aesExchangeKeys: Map<String, Map<String, Map<String, String>>> = emptyMap(),
	override val transferKeys: Map<String, Map<String, String>> = emptyMap(),
	override val privateKeyShamirPartitions: Map<String, String> = emptyMap(),
	override val publicKey: String? = null,
	override val publicKeysForOaepWithSha256: Set<String> = emptySet(),
	override val secretForeignKeys: Set<String> = emptySet(),
	override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	override val encryptedSelf: String? = null,
	override val securityMetadata: SecurityMetadataDto? = null,
	override val medicalLocationId: String? = null,
	override val cryptoActorProperties: Set<PropertyStubDto>? = null,
	@get:Deprecated("Do not use") val nonDuplicateIds: Set<String> = emptySet(),
	@get:Deprecated("Do not use") val encryptedAdministrativesDocuments: Set<String> = emptySet(),
	@get:Deprecated("Use note or administrativeNote") val comment: String? = null,
	@get:Deprecated("Use note or administrativeNote") val warning: String? = null,
	@get:Deprecated("Use properties instead") val fatherBirthCountry: CodeStubDto? = null, // Deprecated won't work on $ref, because the serialisation gets rid of everything that is not $ref (in conformance with the spec)
	@get:Deprecated("Use properties instead") val birthCountry: CodeStubDto? = null, // Deprecated won't work on $ref, because the serialisation gets rid of everything that is not $ref (in conformance with the spec)
	@get:Deprecated("Use properties instead") val nativeCountry: CodeStubDto? = null, // Deprecated won't work on $ref, because the serialisation gets rid of everything that is not $ref (in conformance with the spec)
	@get:Deprecated("Use properties instead") val socialStatus: CodeStubDto? = null, // Deprecated won't work on $ref, because the serialisation gets rid of everything that is not $ref (in conformance with the spec)
	@get:Deprecated("Use properties instead") val mainSourceOfIncome: CodeStubDto? = null, // Deprecated won't work on $ref, because the serialisation gets rid of everything that is not $ref (in conformance with the spec)
	@get:Deprecated("Use properties instead") val schoolingInfos: List<SchoolingInfoDto> = emptyList(),
	@get:Deprecated("Use properties instead") val employementInfos: List<EmploymentInfoDto> = emptyList(),
	override val parentId: Nothing? = null,
) : StoredDocumentDto,
	ICureDocumentDto<String>,
	PersonDto,
	HasEncryptionMetadataDto,
	EncryptableDto,
	CryptoActorDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
