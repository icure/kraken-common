/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.CryptoActor
import org.taktik.icure.entities.base.DataOwner
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.Person
import org.taktik.icure.entities.base.PropertyStub
import org.taktik.icure.entities.base.StoredICureDocument
import org.taktik.icure.entities.embed.AccessLevel
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.entities.embed.Annotation
import org.taktik.icure.entities.embed.DeactivationReason
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.EmploymentInfo
import org.taktik.icure.entities.embed.Encryptable
import org.taktik.icure.entities.embed.FinancialInstitutionInformation
import org.taktik.icure.entities.embed.Gender
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.Insurability
import org.taktik.icure.entities.embed.MedicalHouseContract
import org.taktik.icure.entities.embed.Partnership
import org.taktik.icure.entities.embed.PatientHealthCareParty
import org.taktik.icure.entities.embed.PersonName
import org.taktik.icure.entities.embed.PersonalStatus
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.SchoolingInfo
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.entities.utils.MergeUtil.mergeListsDistinct
import org.taktik.icure.handlers.JacksonBase64LenientDeserializer
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)

/**
 * A patient
 *
 * This entity is a root level object. It represents a patient It is serialized in JSON and saved in the underlying icure-patient CouchDB database.
 * A Patient conforms to a series of interfaces:
 * - StoredICureDocument
 * - Person
 * - Encryptable
 * - CryptoActor
 *
 * @property id The Id of the patient. We encourage using either a v4 UUID or a HL7 Id.
 * @property rev The revision of the patient in the database, used for conflict management / optimistic locking.
 * @property identifier The patient's identifier.
 * @property created The timestamp (unix epoch in ms) of creation of the patient. Enforced by the application server : will be filled automatically if missing.
 * @property modified the date (unix epoch in ms) of latest modification of the patient. Enforced by the application server : will be filled automatically if missing.
 * @property author the id of the User that has created this patient, will be filled automatically if missing. Not enforced by the application server.
 * @property responsible the id of the HealthcareParty that is responsible for this patient, will be filled automatically if missing. Not enforced by the application server.
 * @property medicalLocationId the medical location where this patient has been created
 * @property tags tags that qualify the patient as being member of a certain class.
 * @property codes codes that identify or qualify this particular patient.
 * @property endOfLife soft delete (unix epoch in ms) timestamp of the object. Unused for patient.
 * @property deletionDate Hard delete (unix epoch in ms) timestamp of the object. Filled automatically when deletePatient is called.
 * @property firstName the firstname (name) of the patient.
 * @property lastName the lastname (surname) of the patient. This is the official lastname that should be used for official administrative purposes.
 * @property names The list of all names of the patient, also containing the official full name information. Ordered by preference of use. First element is therefore the official name used for the patient in the application.
 * @property companyName the name of the company this patient is member of.
 * @property languages the list of languages spoken by the patient ordered by fluency (alpha-2 code http://www.loc.gov/standards/iso639-2/ascii_8bits.html).
 * @property addresses the list of addresses (with address type).
 * @property civility Mr., Ms., Pr., Dr. ...
 * @property gender the gender of the patient: male, female, indeterminate, changed, changedToMale, changedToFemale, unknown
 * @property mergeToPatientId The id of the patient this patient has been merged with.
 * @property mergedIds The ids of the patients that have been merged towards this patient.
 * @property alias An alias of the person, nickname, ...
 * @property active Is the patient active (boolean).
 * @property deactivationReason When not active, the reason for deactivation.
 * @property ssin Social security inscription number.
 * @property maidenName Lastname at birth (can be different of the current name), depending on the country, must be used to design the patient .
 * @property spouseName Lastname of the spouse for a married woman, depending on the country, can be used to design the patient.
 * @property partnerName Lastname of the partner, should not be used to design the patient.
 * @property personalStatus any of `single`, `in_couple`, `married`, `separated`, `divorced`, `divorcing`, `widowed`, `widower`, `complicated`, `unknown`, `contract`, `other`.
 * @property dateOfBirth The birthdate encoded as a fuzzy date on 8 positions (YYYYMMDD) MM and/or DD can be set to 00 if unknown (19740000 is a valid date).
 * @property dateOfDeath The date of death encoded as a fuzzy date on 8 positions (YYYYMMDD) MM and/or DD can be set to 00 if unknown (19740000 is a valid date).
 * @property timestampOfLatestEidReading Timestamp of the latest validation of the eID of the person..
 * @property placeOfBirth The place of birth.
 * @property placeOfDeath The place of death.
 * @property education The level of education (college degree, undergraduate, phd).
 * @property profession The current professional activity.
 * @property note A text note (can be confidential, encrypted by default).
 * @property administrativeNote An administrative note, not confidential.
 * @property nationality The nationality of the patient.
 * @property preferredUserId The id of the user that usually handles this patient.
 * @property picture A picture usually saved in JPEG format.
 * @property externalId An external (from another source) id with no guarantee or requirement for unicity .
 * @property insurabilities List of insurance coverages (of class Insurability, see below).
 * @property partnerships List of partners, or persons of contact (of class Partnership, see below).
 * @property patientHealthCareParties Links (usually for therapeutic reasons) between this patient and healthcare parties (of class PatientHealthcareParty).
 * @property financialInstitutionInformation Financial information (Bank, bank account) used to reimburse the patient.
 * @property medicalHouseContracts Contracts between the patient and the healthcare entity.
 * @property patientProfessions Codified list of professions exercised by this patient.
 * @property properties Extra properties for the patient. Those properties are typed (see class Property)
 * @property hcPartyKeys When a patient has access to the medical file for modification or has been given access to it (any time he/she acts as a Crypto Actor), the list of exchange keys with other healthcare parties.
 * @property privateKeyShamirPartitions A set of shamir partitions for that patient RSA private keys, encrypted with the public keys of the notaries (referred by their ids)
 * @property publicKey The public RSA key of this patient
 * @property publicKeysForOaepWithSha256 The public keys of this actor which should be used for RSA-OAEP with sha256 encryption
 * @property delegations The delegations giving access to all connected healthcare information.
 * @property encryptionKeys The patient secret encryption key used to encrypt the secured properties (like note for example), encrypted for separate Crypto Actors.
 * @property encryptedSelf The encrypted fields of this patient.
 *
 */
data class Patient(
	@param:ContentValue(ContentValues.UUID) @JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	val identifier: List<Identifier> = listOf(),
	override val created: Long? = null,
	override val modified: Long? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	override val endOfLife: Long? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,
	@param:ContentValue(ContentValues.ANY_STRING) override val firstName: String? = null,
	@param:ContentValue(ContentValues.ANY_STRING) override val lastName: String? = null, // Is usually either maidenName or spouseName,
	override val companyName: String? = null,
	override val languages: List<String> = emptyList(), // alpha-2 code http://www.loc.gov/standards/iso639-2/ascii_8bits.html,
	@param:ContentValue(ContentValues.NESTED_ENTITIES_LIST)override val addresses: List<Address> = emptyList(),
	override val civility: String? = null,
	override val gender: Gender? = Gender.unknown,
	override val names: List<PersonName> = emptyList(),
	val birthSex: Gender? = Gender.unknown,
	val mergeToPatientId: String? = null,
	val mergedIds: Set<String> = emptySet(),
	val alias: String? = null,
	val active: Boolean = true,
	val deactivationReason: DeactivationReason = DeactivationReason.none,
	val deactivationDate: Int? = null,
	val ssin: String? = null,
	val maidenName: String? = null, // Never changes (nom de jeune fille),
	val spouseName: String? = null, // Name of the spouse after marriage,
	val partnerName: String? = null, // Name of the partner, sometimes equal to spouseName,
	val personalStatus: PersonalStatus? = PersonalStatus.unknown,
	val dateOfBirth: Int? = null, // YYYYMMDD if unknown, 00, ex:20010000 or,
	@param:ContentValue(ContentValues.ANY_BOOLEAN) val deceased: Boolean? = null,
	val dateOfDeath: Int? = null, // YYYYMMDD if unknown, 00, ex:20010000 or,
	val timestampOfLatestEidReading: Long? = null,
	val placeOfBirth: String? = null,
	val placeOfDeath: String? = null,
	val education: String? = null,
	val profession: String? = null,
	val notes: List<Annotation> = emptyList(),
	@Deprecated("Use notes instead") val note: String? = null,
	@Deprecated("Use notes instead") val administrativeNote: String? = null,
	val race: String? = null,
	val ethnicity: String? = null,
	val nationality: String? = null,
	val preferredUserId: String? = null,
	@JsonDeserialize(using = JacksonBase64LenientDeserializer::class) val picture: ByteArray? = null,
	val externalId: String? = null, // No guarantee of unicity
	@param:ContentValue(ContentValues.NESTED_ENTITIES_LIST) val insurabilities: List<Insurability> = emptyList(),
	val partnerships: List<Partnership> = emptyList(),
	val patientHealthCareParties: List<PatientHealthCareParty> = emptyList(),
	val financialInstitutionInformation: List<FinancialInstitutionInformation> = emptyList(),
	val medicalHouseContracts: List<MedicalHouseContract> = emptyList(),
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) val patientProfessions: List<CodeStub> = emptyList(),
	val parameters: Map<String, List<String>> = emptyMap(),
	@Deprecated("Do not use") val nonDuplicateIds: Set<String> = emptySet(),
	@Deprecated("Do not use") val encryptedAdministrativesDocuments: Set<String> = emptySet(),
	@Deprecated("Use note or administrativeNote") val comment: String? = null,
	@Deprecated("Use note or administrativeNote") val warning: String? = null,
	@Deprecated("Use properties instead") val fatherBirthCountry: CodeStub? = null,
	@Deprecated("Use properties instead") val birthCountry: CodeStub? = null,
	@Deprecated("Use properties instead") val nativeCountry: CodeStub? = null,
	@Deprecated("Use properties instead") val socialStatus: CodeStub? = null,
	@Deprecated("Use properties instead") val mainSourceOfIncome: CodeStub? = null,
	@Deprecated("Use properties instead") val schoolingInfos: List<SchoolingInfo> = emptyList(),
	@Deprecated("Use properties instead") val employementInfos: List<EmploymentInfo> = emptyList(),
	override val properties: Set<PropertyStub> = emptySet(),

	// One AES key per HcParty, encrypted using this hcParty public key and the other hcParty public key
	// For a pair of HcParties, this key is called the AES exchange key
	// Each HcParty always has one AES exchange key for himself
	// The map's keys are the delegate id.
	// In the table, we get at the first position: the key encrypted using owner (this)'s public key and in 2nd pos.
	// the key encrypted using delegate's public key.
	override val hcPartyKeys: Map<String, List<String>> = emptyMap(),
	// Extra AES exchange keys, usually the ones we lost access to at some point
	// The structure is { publicKey: { delegateId: { myPubKey1: aesExKey_for_this, delegatePubKey1: aesExKey_for_delegate } } }
	override val aesExchangeKeys: Map<String, Map<String, Map<String, String>>> = emptyMap(),
	// Our private keys encrypted with our public keys
	// The structure is { publicKey1: { publicKey2: privateKey2_encrypted_with_publicKey1, publicKey3: privateKey3_encrypted_with_publicKey1 } }
	override val transferKeys: Map<String, Map<String, String>> = emptyMap(),

	override val privateKeyShamirPartitions: Map<String, String> = emptyMap(),
	override val publicKey: String? = null,
	override val publicKeysForOaepWithSha256: Set<String> = emptySet(),

	override val secretForeignKeys: Set<String> = emptySet(),
	override val cryptedForeignKeys: Map<String, Set<Delegation>> = emptyMap(),
	override val delegations: Map<String, Set<Delegation>> = emptyMap(),
	override val encryptionKeys: Map<String, Set<Delegation>> = emptyMap(),
	override val encryptedSelf: String? = null,
	override val securityMetadata: SecurityMetadata? = null,
	override val cryptoActorProperties: Set<PropertyStub>? = null,
	override val medicalLocationId: String? = null,
	override val parentId: Nothing? = null,
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,
) : StoredICureDocument,
	Person,
	HasEncryptionMetadata,
	CryptoActor,
	DataOwner,
	Encryptable {
	companion object : DynamicInitializer<Patient>

	fun merge(other: Patient) = Patient(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: Patient) = super<StoredICureDocument>.solveConflictsWith(other) +
		super<Person>.solveConflictsWith(other) +
		super<HasEncryptionMetadata>.solveConflictsWith(other) +
		super<CryptoActor>.solveConflictsWith(other) +
		super<DataOwner>.solveConflictsWith(other) +
		mapOf(
			"encryptionKeys" to this.encryptionKeys, // Only keep this ones
			"identifier" to mergeListsDistinct(
				this.identifier,
				other.identifier,
				{ a, b -> a.system == b.system && a.value == b.value },
			),
			"birthSex" to (this.birthSex ?: other.birthSex),
			"mergeToPatientId" to (this.mergeToPatientId ?: other.mergeToPatientId),
			"mergedIds" to (other.mergedIds + this.mergedIds),
			"nonDuplicateIds" to (other.nonDuplicateIds + this.nonDuplicateIds),
			"encryptedAdministrativesDocuments" to (other.encryptedAdministrativesDocuments + this.encryptedAdministrativesDocuments),
			"alias" to (this.alias ?: other.alias),
			"active" to (this.active),
			"deactivationReason" to (this.deactivationReason),
			"deactivationDate" to (this.deactivationDate),
			"ssin" to (this.ssin ?: other.ssin),
			"maidenName" to (this.maidenName ?: other.maidenName),
			"spouseName" to (this.spouseName ?: other.spouseName),
			"partnerName" to (this.partnerName ?: other.partnerName),
			"personalStatus" to (this.personalStatus ?: other.personalStatus),
			"dateOfBirth" to (this.dateOfBirth ?: other.dateOfBirth),
			"deceased" to (this.deceased ?: other.deceased),
			"dateOfDeath" to (this.dateOfDeath ?: other.dateOfDeath),
			"placeOfBirth" to (this.placeOfBirth ?: other.placeOfBirth),
			"placeOfDeath" to (this.placeOfDeath ?: other.placeOfDeath),
			"education" to (this.education ?: other.education),
			"profession" to (this.profession ?: other.profession),
			"note" to (this.note ?: other.note),
			"administrativeNote" to (this.administrativeNote ?: other.administrativeNote),
			"comment" to (this.comment ?: other.comment),
			"warning" to (this.warning ?: other.warning),
			"race" to (this.race ?: other.race),
			"ethnicity" to (this.ethnicity ?: other.ethnicity),
			"nationality" to (this.nationality ?: other.nationality),
			"preferredUserId" to (this.preferredUserId ?: other.preferredUserId),
			"picture" to (this.picture ?: other.picture),
			"externalId" to (this.externalId ?: other.externalId),
			"partnerships" to mergeListsDistinct(partnerships, other.partnerships),
			"financialInstitutionInformation" to mergeListsDistinct(financialInstitutionInformation, other.financialInstitutionInformation),
			"medicalHouseContracts" to mergeListsDistinct(medicalHouseContracts, other.medicalHouseContracts),
			"parameters" to (other.parameters + this.parameters),
			"patientProfessions" to mergeListsDistinct(patientProfessions, other.patientProfessions),
			"fatherBirthCountry" to (this.fatherBirthCountry ?: other.fatherBirthCountry),
			"birthCountry" to (this.birthCountry ?: other.birthCountry),
			"nativeCountry" to (this.nativeCountry ?: other.nativeCountry),
			"socialStatus" to (this.socialStatus ?: other.socialStatus),
			"mainSourceOfIncome" to (this.mainSourceOfIncome ?: other.mainSourceOfIncome),
			"schoolingInfos" to mergeListsDistinct(schoolingInfos, other.schoolingInfos),
			"employementInfos" to mergeListsDistinct(employementInfos, other.employementInfos),
			"insurabilities" to mergeListsDistinct(
				insurabilities,
				other.insurabilities,
				{ a, b -> a.insuranceId == b.insuranceId && a.startDate == b.startDate },
				{ a, b -> if (a.endDate != null) a else b },
			),
			"patientHealthCareParties" to mergeListsDistinct(
				patientHealthCareParties,
				other.patientHealthCareParties,
				{ a, b -> a.healthcarePartyId == b.healthcarePartyId && a.type == b.type },
				{ a, b -> a.merge(b) },
			),
		)

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
	override fun withTimestamps(created: Long?, modified: Long?) = when {
		created != null && modified != null -> this.copy(created = created, modified = modified)
		created != null -> this.copy(created = created)
		modified != null -> this.copy(modified = modified)
		else -> this
	}

	override val dataOwnersWithExplicitAccess: Map<String, AccessLevel>
		get() = super.dataOwnersWithExplicitAccess + mapOf(id to AccessLevel.WRITE)

	fun isValidForStore() = firstName != null || lastName != null || encryptedSelf != null || deletionDate != null
}
