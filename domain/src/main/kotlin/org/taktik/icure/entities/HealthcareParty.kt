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
import org.taktik.icure.entities.base.HasCodes
import org.taktik.icure.entities.base.HasTags
import org.taktik.icure.entities.base.Named
import org.taktik.icure.entities.base.Person
import org.taktik.icure.entities.base.PropertyStub
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.entities.embed.FinancialInstitutionInformation
import org.taktik.icure.entities.embed.FlatRateTarification
import org.taktik.icure.entities.embed.Gender
import org.taktik.icure.entities.embed.HealthcarePartyHistoryStatus
import org.taktik.icure.entities.embed.HealthcarePartyStatus
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.PersonName
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.TelecomType
import org.taktik.icure.entities.utils.MergeUtil.mergeListsDistinct
import org.taktik.icure.handlers.JacksonLenientCollectionDeserializer
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)

/**
 * A healthcareParty
 *
 * This entity is a root level object. It represents a healthcare party. It is serialized in JSON and saved in the underlying icure-healthcare CouchDB database.
 * A Healthcare Party conforms to a series of interfaces:
 * - StoredICureDocument
 * - Person
 * - Encryptable
 * - CryptoActor
 *
 * @property id the Id of the healthcare party. We encourage using either a v4 UUID or a HL7 Id.
 * @property rev the revision of the healthcare party in the database, used for conflict management / optimistic locking.
 * @property deletionDate hard delete (unix epoch in ms) timestamp of the object.
 * @property name The full name of the healthcare party, used mainly when the healthcare party is an organization
 * @property firstName the firstname (name) of the healthcare party.
 * @property lastName the lastname (surname) of the healthcare party. This is the official lastname that should be used for official administrative purposes.
 * @property names The list of all names of the healthcare party, also containing the official full name information. Ordered by preference of use. First element is therefore the official name used for the healthcare party in the application.
 * @property gender the gender of the healthcare party: male, female, indeterminate, changed, changedToMale, changedToFemale, unknown
 * @property civility Mr., Ms., Pr., Dr. ...
 * @property companyName The name of the company this healthcare party is member of
 * @property speciality Medical specialty of the healthcare party
 * @property invoiceHeader All details included in the invoice header
 * @property cbe Identifier number for institution type if the healthcare party is an enterprise
 * @property ehp Identifier number for the institution if the healthcare party is an organization
 * @property userId The id of the user that usually handles this healthcare party.
 * @property parentId Id of parent of the user representing the healthcare party.
 * @property nihii  National Institute for Health and Invalidity Insurance number assigned to healthcare parties (institution or person).
 * @property ssin Social security inscription number.
 * @property addresses The list of addresses (with address type).
 * @property languages The list of languages spoken by the patient ordered by fluency (alpha-2 code http://www.loc.gov/standards/iso639-2/ascii_8bits.html).
 * @property picture A picture usually saved in JPEG format.
 * @property statuses The healthcare party's status: 'trainee' or 'withconvention' or 'accredited'
 * @property specialityCodes Medical specialty of the healthcare party codified using FHIR or Kmehr codificaiton scheme
 * @property sendFormats The type of format for contacting the healthcare party, ex: mobile, phone, email, etc.
 * @property notes Text notes.
 * @property financialInstitutionInformation List of financial information (Bank, bank account).
 * @property billingType The invoicing scheme this healthcare party adheres to : 'service fee' or 'flat rate'
 * @property hcPartyKeys When a healthcare party has access to the medical file for modification or has been given access to it (any time he/she acts as a Crypto Actor), the list of exchange keys with other healthcare parties.
 * @property privateKeyShamirPartitions A set of shamir partitions for this healthcare party RSA private keys, encrypted with the public keys of the notaries (referred by their ids). Format is hcpId of key that has been partitioned : "threshold|partition in hex"
 * @property publicKey The public RSA key of this healthcare party
 * @property publicKeysForOaepWithSha256 The public keys of this actor which should be used for RSA-OAEP with sha256 encryption
 * @property bankAccount Bank Account identifier of the healhtcare party, IBAN, deprecated, use financial institutions instead
 * @property bic Bank Identifier Code, the SWIFT Address assigned to the bank, use financial institutions instead
 * @property descr A description of the HCP, meant for the public and in multiple languages
 * @property identifier The healthcareparty's identifiers, used by the client to identify uniquely and unambiguously the HCP. However, iCure may not guarantee this uniqueness by itself : This should be done at the client side.
 * @property tags Tags that qualify the healthcareparty as being member of a certain class.
 * @property codes Codes that identify or qualify this particular healthcareparty.
 */

data class HealthcareParty(
	@param:ContentValue(ContentValues.UUID) @JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,
	@field:NotNull(autoFix = AutoFix.NOW) val created: Long? = null,
	@field:NotNull(autoFix = AutoFix.NOW) val modified: Long? = null,

	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	val identifier: List<Identifier> = emptyList(),

	override val name: String? = null,
	@param:ContentValue(ContentValues.ANY_STRING) override val lastName: String? = null,
	@param:ContentValue(ContentValues.ANY_STRING) override val firstName: String? = null,
	override val gender: Gender? = null,
	override val civility: String? = null,
	override val companyName: String? = null,
	override val names: List<PersonName> = emptyList(),
	val speciality: String? = null,
	val bankAccount: String? = null,
	val bic: String? = null,
	val proxyBankAccount: String? = null,
	val proxyBic: String? = null,
	val invoiceHeader: String? = null,
	val cbe: String? = null,
	val ehp: String? = null,
	val userId: String? = null,
	override val parentId: String? = null,
	val convention: Int? = null, // 0,1,2,9
	val nihii: String? = null, // institution, person
	val nihiiSpecCode: String? = null, // don't show field in the GUI
	val ssin: String? = null,
	@param:ContentValue(ContentValues.NESTED_ENTITIES_LIST) override val addresses: List<Address> = emptyList(),
	override val languages: List<String> = emptyList(),
	val picture: ByteArray? = null,
	val statuses: Set<HealthcarePartyStatus> = emptySet(),
	val statusHistory: List<HealthcarePartyHistoryStatus> = emptyList(),
	val descr: Map<String, String>? = emptyMap(),
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) @JsonDeserialize(using = JacksonLenientCollectionDeserializer::class) val specialityCodes: Set<CodeStub> = emptySet(), // Speciality codes, default is first

	val sendFormats: Map<TelecomType, String> = emptyMap(),
	val notes: String? = null,
	val financialInstitutionInformation: List<FinancialInstitutionInformation> = emptyList(),

	// Medical houses
	var billingType: String? = null, // "serviceFee" (à l'acte) or "flatRate" (forfait)
	var type: String? = null, // "persphysician" or "medicalHouse" or "perstechnician"
	var contactPerson: String? = null,
	var contactPersonHcpId: String? = null,
	var supervisorId: String? = null,
	var flatRateTarifications: List<FlatRateTarification> = emptyList(),
	var importedData: Map<String, String> = emptyMap(),

	@Deprecated("Use properties instead")
	val options: Map<String, String> = emptyMap(),
	override val properties: Set<PropertyStub> = emptySet(),
	/**
	 * If set to true the healthcare party is considered public, and a stripped down version of the healthcare party
	 * will be accessible through the anonymous endpoints.
	 */
	@param:JsonInclude(JsonInclude.Include.NON_DEFAULT) val public: Boolean = false,
	// One AES key per HcParty, encrypted using this hcParty public key and the other hcParty public key
	// For a pair of HcParties, this key is called the AES exchange key
	// Each HcParty always has one AES exchange key for himself
	// The map's keys are the delegate id.
	// In the table, we get at the first position: the key encrypted using owner (this)'s public key and in 2nd pos.
	// the key encrypted using delegate's public key.
	override val hcPartyKeys: Map<String, List<String>> = emptyMap(),
	// Extra AES exchange keys, usually the ones we lost access to at some point
	// The structure is { publicKey: { delegateId: [aesExKey_for_this, aesExKey_for_delegate] } }
	override val aesExchangeKeys: Map<String, Map<String, Map<String, String>>> = emptyMap(),
	// Our private keys encrypted with our public keys
	// The structure is { publicKey1: { publicKey2: privateKey2_encrypted_with_publicKey1, publicKey3: privateKey3_encrypted_with_publicKey1 } }
	override val transferKeys: Map<String, Map<String, String>> = emptyMap(),
	override val privateKeyShamirPartitions: Map<String, String> = emptyMap(), // Format is hcpId of key that has been partitioned : "threshold|partition in hex"
	override val publicKey: String? = null,
	override val publicKeysForOaepWithSha256: Set<String> = emptySet(),
	override val cryptoActorProperties: Set<PropertyStub>? = null,

	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,

) : StoredDocument,
	Named,
	Person,
	CryptoActor,
	DataOwner,
	HasTags,
	HasCodes {
	companion object : DynamicInitializer<HealthcareParty>

	fun merge(other: HealthcareParty) = HealthcareParty(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: HealthcareParty) = super<StoredDocument>.solveConflictsWith(other) +
		super<Person>.solveConflictsWith(other) +
		super<CryptoActor>.solveConflictsWith(other) +
		mapOf(
			"speciality" to (this.speciality ?: other.speciality),
			"bankAccount" to (this.bankAccount ?: other.bankAccount),
			"bic" to (this.bic ?: other.bic),
			"proxyBankAccount" to (this.proxyBankAccount ?: other.proxyBankAccount),
			"proxyBic" to (this.proxyBic ?: other.proxyBic),
			"invoiceHeader" to (this.invoiceHeader ?: other.invoiceHeader),
			"cbe" to (this.cbe ?: other.cbe),
			"ehp" to (this.ehp ?: other.ehp),
			"userId" to (this.userId ?: other.userId),
			"parentId" to (this.parentId ?: other.parentId),
			"convention" to (this.convention ?: other.convention),
			"nihii" to (this.nihii ?: other.nihii),
			"nihiiSpecCode" to (this.nihiiSpecCode ?: other.nihiiSpecCode),
			"ssin" to (this.ssin ?: other.ssin),
			"picture" to (this.picture ?: other.picture),
			"statuses" to (other.statuses + this.statuses),
			"specialityCodes" to (other.specialityCodes + this.specialityCodes),
			"sendFormats" to (other.sendFormats + this.sendFormats),
			"notes" to (this.notes ?: other.notes),
			"descr" to (this.descr ?: other.descr),
			"financialInstitutionInformation" to mergeListsDistinct(
				this.financialInstitutionInformation,
				other.financialInstitutionInformation,
				{ a, b -> a.key?.equals(b.key) ?: false },
			),
			"billingType" to (this.billingType ?: other.billingType),
			"type" to (this.type ?: other.type),
			"contactPerson" to (this.contactPerson ?: other.contactPerson),
			"contactPersonHcpId" to (this.contactPersonHcpId ?: other.contactPersonHcpId),
			"flatRateTarifications" to mergeListsDistinct(
				this.flatRateTarifications,
				other.flatRateTarifications,
				{ a, b -> a.flatRateType?.equals(b.flatRateType) ?: false },
			),
			"importedData" to (other.importedData + this.importedData),
			"options" to (other.options + this.options),
			"identifier" to mergeListsDistinct(
				this.identifier,
				other.identifier,
				{ a, b -> a.system == b.system && a.value == b.value },
			),
		)

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
