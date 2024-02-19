/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.Encryptable
import org.taktik.icure.entities.base.ICureDocument
import org.taktik.icure.entities.base.LinkQualification
import org.taktik.icure.entities.base.hasDataOwnerOrDelegationKey
import org.taktik.icure.entities.utils.Base64String
import org.taktik.icure.serializers.ServiceQualifiedLinkDeserializer
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode
import java.util.*

/**
 * This entity represents a Service.
 * A Service is created in the course a contact. Information like temperature, blood pressure, etc. are the temporal structural element of a medical file.
 * A contact contains a series of services (acts, observations, exchanges) performed on the patient. These services can be linked to healthcare elements
 *
 * A Service conforms to a series of interfaces:
 * - StoredICureDocument
 * - Encryptable
 *
 * @property id The Id of the Service. We encourage using either a v4 UUID or a HL7 Id.
 * @property identifier The identifier of the Service.
 * @property contactId Id of the contact during which the service is provided
 * @property subContactIds List of IDs of all sub-contacts that link the service to structural elements. Only used when the Service is emitted outside its contact
 * @property plansOfActionIds List of IDs of all plans of actions (healthcare approaches) as a part of which the Service is provided. Only used when the Service is emitted outside its contact
 * @property healthElementsIds List of IDs of all healthcare elements for which the service is provided. Only used when the Service is emitted outside its contact
 * @property formIds List of Ids of all forms linked to the Service. Only used when the Service is emitted outside its contact.
 * @property secretForeignKeys The secret patient key, encrypted in the patient document, in clear here. Only used when the Service is emitted outside its contact
 * @property cryptedForeignKeys The public patient key, encrypted here for separate Crypto Actors. Only used when the Service is emitted outside its contact
 * @property delegations The delegations giving access to connected healthcare information. Only used when the Service is emitted outside its contact
 * @property encryptionKeys The contact secret encryption key used to encrypt the secured properties (like services for example), encrypted for separate Crypto Actors. Only used when the Service is emitted outside its contact
 * @property label Description / Unambiguous qualification (LOINC code) of the type of information contained in the service. Could be a code to qualify temperature, complaint, diagnostic, ...
 * @property dataClassName Deprecated : Do not use it anymore
 * @property index Used for sorting services inside an upper object (A contact, a transaction, a FHIR bundle, ...)
 * @property content Information contained in the service. Content is localized, using ISO language code as key.
 * @property textIndexes
 * @property valueDate The date (YYYYMMDDhhmmss) when the Service is noted to have started and also closes on the same date
 * @property openingDate The date (YYYYMMDDhhmmss) of the start of the Service.
 * @property closingDate The date (YYYYMMDDhhmmss) marking the end of the Service.
 * @property formId Id of the form used during the Service.
 * @property created The timestamp (unix epoch in ms) of creation of the Service, will be filled automatically if missing. Not enforced by the application server.
 * @property modified The date (unix epoch in ms) of the latest modification of the Service, will be filled automatically if missing. Not enforced by the application server.
 * @property endOfLife Soft delete (unix epoch in ms) timestamp of the object.
 * @property author The id of the User that has created this service, will be filled automatically if missing. Not enforced by the application server.
 * @property responsible The id of the HealthcareParty that is responsible for this service, will be filled automatically if missing. Not enforced by the application server.
 * @property medicalLocationId The id of the medical location where the service was recorded.
 * @property comment Text, comments on the Service provided
 * @property invoicingCodes List of invoicing codes
 * @property notes Comments - Notes recorded by a HCP about this service
 * @property qualifiedLinks Links towards related services (possibly in other contacts)
 * @property codes Codes that identify or qualify this particular service.
 * @property tags Tags that qualify the service as being member of a certain class.
 * @property encryptedSelf The encrypted fields of this service.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Service(
	@param:ContentValue(ContentValues.UUID) @JsonProperty("_id") override val id: String = UUID.randomUUID().toString(),
	val transactionId: String? = null, //Used when a single service had to be split into parts for technical reasons. Several services with the same non null transaction id form one single service
	val identifier: List<Identifier> = emptyList(),
	@JsonIgnore val subContactIds: Set<String>? = null, //Only used when the Service is emitted outside its contact
	@JsonIgnore val plansOfActionIds: Set<String>? = null, //Only used when the Service is emitted outside its contact
	@JsonIgnore val healthElementsIds: Set<String>? = null, //Only used when the Service is emitted outside its contact
	@JsonIgnore val formIds: Set<String>? = null, //Only used when the Service is emitted outside its contact
	@JsonIgnore val secretForeignKeys: Set<String>? = null, //Only used when the Service is emitted outside its contact
	@JsonIgnore val cryptedForeignKeys: Map<String, Set<Delegation>> = emptyMap(), //Only used when the Service is emitted outside its contact
	@JsonIgnore val delegations: Map<String, Set<Delegation>> = emptyMap(), //Only used when the Service is emitted outside its contact
	@JsonIgnore val encryptionKeys: Map<String, Set<Delegation>> = emptyMap(), //Only used when the Service is emitted outside its contact
	@JsonIgnore val contactId: String? = null, //Only used when the Service is emitted outside its contact
	@JsonIgnore val securityMetadata: SecurityMetadata? = null,  //Only used when the Service is emitted outside its contact
	val label: String? = null,
	@Deprecated("Deleted in V2") val dataClassName: String? = null,
	val index: Long? = null, //Used for sorting
	@param:ContentValue(ContentValues.LOCALIZED_NESTED_ENTITIES) val content: Map<String /* ISO language code */, Content> = emptyMap(), //Localized, in the case when the service contains a document, the document id is the SerializableValue
	@Deprecated("use encryptedSelf instead") val encryptedContent: String? = null, //Crypted (AES+base64) version of the above, deprecated, use encryptedSelf instead
	val textIndexes: Map<String, String> = emptyMap(), //Same structure as content but used for full text indexation
	@param:ContentValue(ContentValues.FUZZY_DATE) @field:NotNull(autoFix = AutoFix.FUZZYNOW) val valueDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20140101235960.
	@param:ContentValue(ContentValues.FUZZY_DATE) @field:NotNull(autoFix = AutoFix.FUZZYNOW) val openingDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20140101235960.
	@param:ContentValue(ContentValues.FUZZY_DATE) val closingDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20140101235960.
	val formId: String? = null, //Used to group logically related services - legacy, use subContacts
	@param:ContentValue(ContentValues.TIMESTAMP) @field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	@param:ContentValue(ContentValues.TIMESTAMP) @field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	override val endOfLife: Long? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID) override val author: String? = null, //userId
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID) override val responsible: String? = null, //healthcarePartyId
	override val medicalLocationId: String? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val comment: String? = null,
	val status: Int? = null, //bit 0: active/inactive, bit 1: relevant/irrelevant, bit2 : present/absent, ex: 0 = active,relevant and present
	val invoicingCodes: Set<String> = emptySet(),
	val notes: List<Annotation> = emptyList(),
	@JsonDeserialize(using = ServiceQualifiedLinkDeserializer::class) val qualifiedLinks: Map<LinkQualification, Map<String, String>> = emptyMap(), //Links towards related services (possibly in other contacts)
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(), //stub object of the Code used to qualify the content of the Service
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(), //stub object of the tag used to qualify the type of the Service
	override val encryptedSelf: String? = null,
	) : Encrypted, ICureDocument<String>, Comparable<Service> {
	companion object : DynamicInitializer<Service>

	fun merge(other: Service) = Service(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: Service) = super<Encrypted>.solveConflictsWith(other) + super<ICureDocument>.solveConflictsWith(other) + mapOf(
		"label" to if (this.label.isNullOrBlank()) other.label else this.label,
		"dataClassName" to (this.dataClassName ?: other.dataClassName),
		"index" to (this.index ?: other.index),
		"contactId" to (this.contactId ?: other.contactId),
		"content" to (other.content + this.content),
		"encryptedContent" to (this.encryptedContent ?: other.encryptedContent),
		"textIndexes" to (other.textIndexes + this.textIndexes),
		"valueDate" to (valueDate?.coerceAtMost(other.valueDate ?: Long.MAX_VALUE) ?: other.valueDate),
		"openingDate" to (openingDate?.coerceAtMost(other.openingDate ?: Long.MAX_VALUE) ?: other.openingDate),
		"closingDate" to (closingDate?.coerceAtLeast(other.closingDate ?: 0L) ?: other.closingDate),
		"formId" to (this.formId ?: other.formId),
		"author" to (this.author ?: other.author),
		"responsible" to (this.responsible ?: other.responsible),
		"comment" to (this.comment ?: other.comment),
		"status" to (this.status ?: other.status),
		"invoicingCodes" to (other.invoicingCodes + this.invoicingCodes),
		"notes" to (other.notes + this.notes),
		"qualifiedLinks" to (other.qualifiedLinks + this.qualifiedLinks)
	)

	override fun compareTo(other: Service): Int {
		if (this == other) {
			return 0
		}
		var idx = if (index != null && other.index != null) index.compareTo(other.index) else 0
		if (idx != 0) return idx
		idx = id.compareTo(other.id)
		return if (idx != 0) idx else 1
	}

	override fun withTimestamps(created: Long?, modified: Long?) =
		when {
			created != null && modified != null -> this.copy(created = created, modified = modified)
			created != null -> this.copy(created = created)
			modified != null -> this.copy(modified = modified)
			else -> this
		}
}

private data class EncryptableServiceStub(
	override val secretForeignKeys: Set<String>,
	override val cryptedForeignKeys: Map<String, Set<Delegation>>,
	override val delegations: Map<String, Set<Delegation>>,
	override val encryptionKeys: Map<String, Set<Delegation>>,
	override val encryptedSelf: Base64String?,
	override val securityMetadata: SecurityMetadata?
): Encryptable

/**
 * If the service is 'pimped' with contact information returns the service as an encryptable entity stub, allowing it
 * to be used with any [Encryptable] methods.
 */
fun Service.asEncryptable(): Encryptable? = if (
	contactId != null
) EncryptableServiceStub(
	secretForeignKeys = secretForeignKeys ?: emptySet(),
	cryptedForeignKeys = cryptedForeignKeys,
	delegations = delegations,
	encryptionKeys = encryptionKeys,
	encryptedSelf = encryptedSelf,
	securityMetadata = securityMetadata
) else null

/**
 * If [this] is a pimped service works as [Encryptable.hasDataOwnerOrDelegationKey], else returns false.
 */
fun Service.hasDataOwnerOrDelegationKey(dataOwnerIdOrDelegationKey: String): Boolean =
	asEncryptable()?.hasDataOwnerOrDelegationKey(dataOwnerIdOrDelegationKey) ?: false
