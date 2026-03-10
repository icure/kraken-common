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
import org.taktik.icure.SdkNonNullable
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.CryptoActorDto
import org.taktik.icure.services.external.rest.v2.dto.base.DataOwnerDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasCodesDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasTagsDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.base.NamedDto
import org.taktik.icure.services.external.rest.v2.dto.base.PersonDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AddressDto
import org.taktik.icure.services.external.rest.v2.dto.embed.FinancialInstitutionInformationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.FlatRateTarificationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.GenderDto
import org.taktik.icure.services.external.rest.v2.dto.embed.HealthcarePartyHistoryStatusDto
import org.taktik.icure.services.external.rest.v2.dto.embed.HealthcarePartyStatusDto
import org.taktik.icure.services.external.rest.v2.dto.embed.PersonNameDto
import org.taktik.icure.services.external.rest.v2.dto.embed.TelecomTypeDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.AesExchangeKeyEncryptionKeypairIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.AesExchangeKeyEntryKeyStringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.HexStringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.SpkiHexStringDto

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
	description = """This entity is a root level object. It represents a healthcare party. It is serialized in JSON and saved in the underlying icure-healthdata CouchDB database.""",
)
/**
 * Represents a healthcare party. A healthcare party is a person or organization that provides healthcare services,
 * such as a physician, nurse, hospital, or medical practice. It is serialized in JSON and saved in the underlying
 * icure-healthdata CouchDB database.
 */
data class HealthcarePartyDto(
	/** The Id of the healthcare party. We encourage using either a v4 UUID or a HL7 Id. */
	@param:Schema(description = "the Id of the healthcare party. We encourage using either a v4 UUID or a HL7 Id.")
	override val id: String,
	/** The revision of the healthcare party in the database, used for conflict management / optimistic locking. */
	@param:Schema(description = "the revision of the healthcare party in the database, used for conflict management / optimistic locking.")
	override val rev: String? = null,
	/** Creation timestamp (unix epoch in ms) of the object. */
	@param:Schema(description = "creation timestamp of the object.") val created: Long? = null,
	/** Last modification timestamp (unix epoch in ms) of the object. */
	@param:Schema(description = "last modification timestamp of the object.") val modified: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	@param:Schema(description = "hard delete (unix epoch in ms) timestamp of the object.") override val deletionDate: Long? = null,
	@param:Schema(
		description = "The healthcareparty's identifiers, used by the client to identify uniquely and unambiguously the HCP. However, iCure may not guarantee this uniqueness by itself : This should be done at the client side.",
	/** The healthcare party's identifiers, used by the client to identify uniquely and unambiguously the HCP. */
	) val identifier: List<IdentifierDto> = emptyList(),
	/** Tags that qualify the healthcare party as being member of a certain class. */
	@param:Schema(description = "Tags that qualify the healthcareparty as being member of a certain class.") override val tags: Set<CodeStubDto> =
		emptySet(),
	/** Codes that identify or qualify this particular healthcare party. */
	@param:Schema(description = "Codes that identify or qualify this particular healthcareparty.") override val codes: Set<CodeStubDto> = emptySet(),
	/** The full name of the healthcare party, used mainly when the healthcare party is an organization. */
	@param:Schema(description = "The full name of the healthcare party, used mainly when the healthcare party is an organization") override val name: String? = null,
	@param:Schema(
		description = "the lastname (surname) of the healthcare party. This is the official lastname that should be used for official administrative purposes.",
	/** The lastname (surname) of the healthcare party. */
	) override val lastName: String? = null,
	/** The firstname (name) of the healthcare party. */
	@param:Schema(description = "the firstname (name) of the healthcare party.") override val firstName: String? = null,
	@param:Schema(
		description = "the list of all names of the healthcare party, also containing the official full name information. Ordered by preference of use. First element is therefore the official name used for the healthcare party in the application",
	/** The list of all names of the healthcare party, ordered by preference of use. */
	) override val names: List<PersonNameDto> = emptyList(),
	@param:Schema(
		description = "the gender of the healthcare party: male, female, indeterminate, changed, changedToMale, changedToFemale, unknown",
	/** The gender of the healthcare party. */
	) override val gender: GenderDto? = null,
	/** Mr., Ms., Pr., Dr. ... */
	@param:Schema(description = "Mr., Ms., Pr., Dr. ...") override val civility: String? = null,
	/** The name of the company this healthcare party is member of. */
	@param:Schema(description = "The name of the company this healthcare party is member of") override val companyName: String? = null,
	/** Medical specialty of the healthcare party. */
	@param:Schema(description = "Medical specialty of the healthcare party") val speciality: String? = null,
	@param:Schema(
		description = "Bank Account identifier of the healhtcare party, IBAN, deprecated, use financial institutions instead",
	/** Bank Account identifier of the healthcare party (IBAN). */
	) val bankAccount: String? = null,
	/** Bank Identifier Code (SWIFT Address) assigned to the bank. */
	@param:Schema(description = "Bank Identifier Code, the SWIFT Address assigned to the bank, use financial institutions instead")
	val bic: String? = null,
	/** Proxy bank account number. */
	val proxyBankAccount: String? = null,
	/** Proxy bank identifier code. */
	val proxyBic: String? = null,
	/** All details included in the invoice header. */
	@param:Schema(description = "All details included in the invoice header") val invoiceHeader: String? = null,
	/** Identifier number for institution type if the healthcare party is an enterprise. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "Identifier number for institution type if the healthcare party is an enterprise") val cbe: String? = null,
	/** Identifier number for the institution if the healthcare party is an organization. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "Identifier number for the institution if the healthcare party is an organization") val ehp: String? = null,
	/** The id of the user that usually handles this healthcare party. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "The id of the user that usually handles this healthcare party.") val userId: String? = null,
	/** The id of the parent healthcare party. */
	override val parentId: String? = null,
	/** The convention number (0, 1, 2, or 9). */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val convention: Int? = null, // 0,1,2,9
	/** National Institute for Health and Invalidity Insurance number. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "National Institute for Health and Invalidity Insurance number assigned to healthcare parties (institution or person).")
	val nihii: String? = null, // institution, person
	/** NIHII specialization code. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val nihiiSpecCode: String? = null, // don't show field in the GUI
	/** Social security inscription number. */
	@param:Schema(description = "Social security inscription number.") val ssin: String? = null,
	/** The list of addresses (with address type). */
	@param:Schema(description = "The list of addresses (with address type).") override val addresses: List<AddressDto> = emptyList(),
	/** The list of languages spoken by the healthcare party ordered by fluency (alpha-2 code). */
	@param:Schema(description = "The list of languages spoken by the patient ordered by fluency (alpha-2 code http://www.loc.gov/standards/iso639-2/ascii_8bits.html).")
	override val languages: List<String> = emptyList(),
	/** A picture usually saved in JPEG format. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "A picture usually saved in JPEG format.", type = "string", format = "byte")
	val picture: ByteArray? = null,
	/** The healthcare party's status: 'trainee' or 'withconvention' or 'accredited'. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "The healthcare party's status: 'trainee' or 'withconvention' or 'accredited'")
	val statuses: Set<HealthcarePartyStatusDto> = emptySet(),
	/** The healthcare party's status history. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "The healthcare party's status history")
	val statusHistory: List<HealthcarePartyHistoryStatusDto> = emptyList(),
	/** Medical specialty of the healthcare party codified using FHIR or Kmehr codification scheme. */
	@param:Schema(description = "Medical specialty of the healthcare party codified using FHIR or Kmehr codificaiton scheme") val specialityCodes: Set<CodeStubDto> = emptySet(), // Speciality codes, default is first
	/** The type of format for contacting the healthcare party. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "The type of format for contacting the healthcare party, ex: mobile, phone, email, etc.")
	val sendFormats: Map<TelecomTypeDto, String> = emptyMap(),
	/** Text notes. */
	@param:Schema(description = "Text notes.") val notes: String? = null,
	/** List of financial information (Bank, bank account). */
	@param:Schema(description = "List of financial information (Bank, bank account).") val financialInstitutionInformation: List<FinancialInstitutionInformationDto> = emptyList(),
	/** A description of the HCP, meant for the public and in multiple languages. */
	@SdkNonNullable
	@param:JsonInclude(JsonInclude.Include.NON_NULL)
	@param:Schema(description = "A description of the HCP, meant for the public and in multiple languages.", defaultValue = "emptyMap()")
	val descr: Map<String, String>? = emptyMap(),
	// Medical houses
	/** The invoicing scheme this healthcare party adheres to: 'service fee' or 'flat rate'. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "The invoicing scheme this healthcare party adheres to : 'service fee' or 'flat rate'")
	var billingType: String? = null, // "serviceFee" (à l'acte) or "flatRate" (forfait)
	/** The type of healthcare party (e.g., 'persphysician', 'medicalHouse', 'perstechnician'). */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val type: String? = null, // "persphysician" or "medicalHouse" or "perstechnician"
	/** Contact person name. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val contactPerson: String? = null,
	/** Contact person healthcare party id. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val contactPersonHcpId: String? = null,
	/** The id of the supervisor. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val supervisorId: String? = null,
	/** List of flat rate tarifications for medical houses. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val flatRateTarifications: List<FlatRateTarificationDto> = emptyList(),
	/** Imported data map. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val importedData: Map<String, String> = emptyMap(),
	/** Additional options (deprecated, use properties instead). */
	@Deprecated("Use properties instead")
	val options: Map<String, String> = emptyMap(),
	/** Extra properties for the healthcare party. */
	override val properties: Set<PropertyStubDto> = emptySet(),
	/** Whether the healthcare party profile is publicly visible. */
	@param:JsonInclude(JsonInclude.Include.NON_DEFAULT) val public: Boolean = false,
	/** Properties that are publicly visible. */
	@param:JsonInclude(JsonInclude.Include.NON_DEFAULT) val publicProperties: Set<PropertyStubDto>? = null,
	/** Properties related to crypto actor functionality. */
	@param:JsonInclude(JsonInclude.Include.NON_NULL)
	@SdkNonNullable
	override val cryptoActorProperties: Set<PropertyStubDto>? = null,
	/** For each couple of HcParties (delegate and owner), this map contains the AES exchange key. */
	override val hcPartyKeys: Map<String, List<HexStringDto>> = emptyMap(),
	/** Extra AES exchange keys, indexed by the owner of the pair and target data owner id. */
	override val aesExchangeKeys: Map<AesExchangeKeyEntryKeyStringDto, Map<String, Map<AesExchangeKeyEncryptionKeypairIdentifierDto, HexStringDto>>> = emptyMap(),
	/** Keys used to transfer ownership of encrypted data between key pairs. */
	override val transferKeys: Map<AesExchangeKeyEncryptionKeypairIdentifierDto, Map<AesExchangeKeyEncryptionKeypairIdentifierDto, HexStringDto>> = emptyMap(),
	/** Shamir partitions of the private key. */
	override val privateKeyShamirPartitions: Map<String, HexStringDto> = emptyMap(), // Format is hcpId of key that has been partitionned : "threshold⎮partition in hex"
	/** The public key of this HCP, used to encrypt data for this HCP. */
	override val publicKey: SpkiHexStringDto? = null,
	/** Public keys for OAEP with SHA-256 encryption. */
	override val publicKeysForOaepWithSha256: Set<SpkiHexStringDto> = emptySet(),
) : StoredDocumentDto,
	NamedDto,
	PersonDto,
	CryptoActorDto,
	DataOwnerDto,
	HasCodesDto,
	HasTagsDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
