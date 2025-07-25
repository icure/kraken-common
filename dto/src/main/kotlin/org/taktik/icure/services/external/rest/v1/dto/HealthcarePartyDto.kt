/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v1.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v1.dto.base.CryptoActorDto
import org.taktik.icure.services.external.rest.v1.dto.base.DataOwnerDto
import org.taktik.icure.services.external.rest.v1.dto.base.HasCodesDto
import org.taktik.icure.services.external.rest.v1.dto.base.HasTagsDto
import org.taktik.icure.services.external.rest.v1.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v1.dto.base.NamedDto
import org.taktik.icure.services.external.rest.v1.dto.base.PersonDto
import org.taktik.icure.services.external.rest.v1.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.embed.AddressDto
import org.taktik.icure.services.external.rest.v1.dto.embed.FinancialInstitutionInformationDto
import org.taktik.icure.services.external.rest.v1.dto.embed.FlatRateTarificationDto
import org.taktik.icure.services.external.rest.v1.dto.embed.GenderDto
import org.taktik.icure.services.external.rest.v1.dto.embed.HealthcarePartyHistoryStatusDto
import org.taktik.icure.services.external.rest.v1.dto.embed.HealthcarePartyStatusDto
import org.taktik.icure.services.external.rest.v1.dto.embed.PersonNameDto
import org.taktik.icure.services.external.rest.v1.dto.embed.TelecomTypeDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
	description = """This entity is a root level object. It represents a healthcare party. It is serialized in JSON and saved in the underlying icure-healthdata CouchDB database.""",
)
data class HealthcarePartyDto(
	@get:Schema(description = "the Id of the healthcare party. We encourage using either a v4 UUID or a HL7 Id.") override val id: String,
	@get:Schema(
		description = "the revision of the healthcare party in the database, used for conflict management / optimistic locking.",
	) override val rev: String? = null,
	@get:Schema(description = "creation timestamp of the object.") val created: Long? = null,
	@get:Schema(description = "last modification timestamp of the object.") val modified: Long? = null,
	@get:Schema(description = "hard delete (unix epoch in ms) timestamp of the object.") override val deletionDate: Long? = null,
	@get:Schema(
		description = "The healthcareparty's identifiers, used by the client to identify uniquely and unambiguously the HCP. However, iCure may not guarantee this uniqueness by itself : This should be done at the client side.",
	) val identifier: List<IdentifierDto> = emptyList(),
	@get:Schema(description = "Tags that qualify the healthcareparty as being member of a certain class.") override val tags: Set<CodeStubDto> =
		emptySet(),
	@get:Schema(description = "Codes that identify or qualify this particular healthcareparty.") override val codes: Set<CodeStubDto> = emptySet(),
	@get:Schema(description = "The full name of the healthcare party, used mainly when the healthcare party is an organization") override val name: String? = null,
	@get:Schema(
		description = "the lastname (surname) of the healthcare party. This is the official lastname that should be used for official administrative purposes.",
	) override val lastName: String? = null,
	@get:Schema(description = "the firstname (name) of the healthcare party.") override val firstName: String? = null,
	@get:Schema(
		description = "the list of all names of the healthcare party, also containing the official full name information. Ordered by preference of use. First element is therefore the official name used for the healthcare party in the application",
	) override val names: List<PersonNameDto> = emptyList(),
	@get:Schema(
		description = "the gender of the healthcare party: male, female, indeterminate, changed, changedToMale, changedToFemale, unknown",
	) override val gender: GenderDto? = null,
	@get:Schema(description = "Mr., Ms., Pr., Dr. ...") override val civility: String? = null,
	@get:Schema(description = "The name of the company this healthcare party is member of") override val companyName: String? = null,
	@get:Schema(description = "Medical specialty of the healthcare party") val speciality: String? = null,
	@get:Schema(
		description = "Bank Account identifier of the healhtcare party, IBAN, deprecated, use financial institutions instead",
	) val bankAccount: String? = null,
	@get:Schema(
		description = "Bank Identifier Code, the SWIFT Address assigned to the bank, use financial institutions instead",
	) val bic: String? =
		null,
	val proxyBankAccount: String? = null,
	val proxyBic: String? = null,
	@get:Schema(description = "All details included in the invoice header") val invoiceHeader: String? = null,
	@get:Schema(description = "Identifier number for institution type if the healthcare party is an enterprise") val cbe: String? = null,
	@get:Schema(description = "Identifier number for the institution if the healthcare party is an organization") val ehp: String? = null,
	@get:Schema(description = "The id of the user that usually handles this healthcare party.") val userId: String? = null,
	override val parentId: String? = null,
	val convention: Int? = null, // 0,1,2,9
	@get:Schema(
		description = "National Institute for Health and Invalidity Insurance number assigned to healthcare parties (institution or person).",
	) val nihii: String? = null, // institution, person
	val nihiiSpecCode: String? = null, // don't show field in the GUI
	@get:Schema(description = "Social security inscription number.") val ssin: String? = null,
	@get:Schema(description = "The list of addresses (with address type).") override val addresses: List<AddressDto> = emptyList(),
	@get:Schema(
		description = "The list of languages spoken by the patient ordered by fluency (alpha-2 code http://www.loc.gov/standards/iso639-2/ascii_8bits.html).",
	) override val languages: List<String> = emptyList(),
	@get:Schema(description = "A picture usually saved in JPEG format.", type = "string", format = "byte") val picture: ByteArray? = null,
	@get:Schema(description = "The healthcare party's status: 'trainee' or 'withconvention' or 'accredited'") val statuses: Set<HealthcarePartyStatusDto> = emptySet(),
	@get:Schema(description = "The healthcare party's status history") val statusHistory: List<HealthcarePartyHistoryStatusDto> = emptyList(),
	@get:Schema(description = "Medical specialty of the healthcare party codified using FHIR or Kmehr codificaiton scheme") val specialityCodes: Set<CodeStubDto> = emptySet(), // Speciality codes, default is first
	@get:Schema(description = "The type of format for contacting the healthcare party, ex: mobile, phone, email, etc.") val sendFormats: Map<TelecomTypeDto, String> = emptyMap(),
	@get:Schema(description = "Text notes.") val notes: String? = null,
	@get:Schema(description = "List of financial information (Bank, bank account).") val financialInstitutionInformation: List<FinancialInstitutionInformationDto> = emptyList(),
	@get:Schema(description = "A description of the HCP, meant for the public and in multiple languages.") val descr: Map<String, String>? =
		emptyMap(),
	// Medical houses
	@get:Schema(
		description = "The invoicing scheme this healthcare party adheres to : 'service fee' or 'flat rate'",
	) var billingType: String? = null, // "serviceFee" (à l'acte) or "flatRate" (forfait)
	val type: String? = null, // "persphysician" or "medicalHouse" or "perstechnician"
	val contactPerson: String? = null,
	val contactPersonHcpId: String? = null,
	val supervisorId: String? = null,
	val flatRateTarifications: List<FlatRateTarificationDto> = emptyList(),
	val importedData: Map<String, String> = emptyMap(),
	@Deprecated("Use properties instead")
	val options: Map<String, String> = emptyMap(),
	override val properties: Set<PropertyStubDto> = emptySet(),
	@param:JsonInclude(JsonInclude.Include.NON_DEFAULT) val public: Boolean = false,
	override val hcPartyKeys: Map<String, List<String>> = emptyMap(),
	override val aesExchangeKeys: Map<String, Map<String, Map<String, String>>> = emptyMap(),
	override val transferKeys: Map<String, Map<String, String>> = emptyMap(),
	override val privateKeyShamirPartitions: Map<String, String> = emptyMap(), // Format is hcpId of key that has been partitionned : "threshold⎮partition in hex"
	override val publicKey: String? = null,
	override val publicKeysForOaepWithSha256: Set<String> = emptySet(),
	override val cryptoActorProperties: Set<PropertyStubDto>? = null,
) : StoredDocumentDto,
	NamedDto,
	PersonDto,
	CryptoActorDto,
	DataOwnerDto,
	HasTagsDto,
	HasCodesDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
