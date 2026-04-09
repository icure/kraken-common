/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.ParticipantType
import org.taktik.icure.entities.base.StoredICureDocument
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.entities.embed.Annotation
import org.taktik.icure.entities.embed.ContactParticipant
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Encryptable
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.entities.embed.ServiceLink
import org.taktik.icure.entities.embed.SubContact
import org.taktik.icure.mergers.annotations.MergeStrategyMax
import org.taktik.icure.mergers.annotations.MergeStrategyMin
import org.taktik.icure.mergers.annotations.Mergeable
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode

/**
 * This entity is a root level object. It represents a contact. It is serialized in JSON and saved in the underlying icure-contact CouchDB database.
 *
 * A contact is an entry in the day to day journal of the medical file of a patient. A contact happens between one patient, one or several healthcare parties (with one healthcare party promoted as the responsible of the contact), at one place during one (fairly short) period of time.
 * A contact contains a series of services (acts, observations, exchanges) performed on the patient. These services can be linked to healthcare elements

 * A Contact conforms to a series of interfaces:
 * - StoredICureDocument
 * - Encryptable
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["id"])
data class Contact(
	/** The Id of the contact. We encourage using either a v4 UUID or a HL7 Id. */
	@param:JsonProperty("_id") override val id: String,
	/** The revision of the contact in the database, used for conflict management / optimistic locking. */
	@param:JsonProperty("_rev") override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation of the contact, will be filled automatically if missing. Not enforced by the application server. */
	@field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	/** The date (unix epoch in ms) of the latest modification of the contact, will be filled automatically if missing. Not enforced by the application server. */
	@field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	/** The id of the User that has created this contact, will be filled automatically if missing. Not enforced by the application server. */
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	/** The id of the HealthcareParty that is responsible for this contact, will be filled automatically if missing. Not enforced by the application server. */
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	/** The id of the medical location where the contact was recorded. Deprecated for use with Cardinal SDK. */
	override val medicalLocationId: String? = null,
	/** Tags that qualify the contact as being member of a certain class. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	/** Codes that identify or qualify this particular contact. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	/** The identifiers of the Contact. */
	val identifier: List<Identifier> = emptyList(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	override val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	@field:JsonProperty("deleted") override val deletionDate: Long? = null,

	/** Separate contacts can merged in one logical contact if they share the same groupId. When a contact must be split to selectively assign rights to healthcare parties, the split contacts all share the same groupId */
	@field:NotNull(autoFix = AutoFix.UUID) val groupId: String? = null,

	/** The date (YYYYMMDDhhmmss) of the start of the contact. */
	@field:NotNull(autoFix = AutoFix.FUZZYNOW)
	@MergeStrategyMin
	val openingDate: Long? = null,
	/** The date (YYYYMMDDhhmmss) marking the end of the contact. */
	@MergeStrategyMax
	val closingDate: Long? = null,

	/** Description of the contact */
	val descr: String? = null,
	/** Location where the contact was recorded. */
	val location: String? = null,
	@Deprecated("Replaced by responsible") val healthcarePartyId: String? = null,
	/** An external (from another source) id with no guarantee or requirement for unicity. */
	val externalId: String? = null,
	@Deprecated("Contacts should be linked together using formId in subcontact") val modifiedContactId: String? = null,
	/** The type of encounter made for the contact */
	val encounterType: CodeStub? = null,
	/** The location where the encounter took place. */
	val encounterLocation: Address? = null,
	/** Set of all sub-contacts recorded during the given contact. Sub-contacts are used to link services embedded inside this contact to healthcare elements, healthcare approaches and/or forms. */
	@field:Valid val subContacts: Set<SubContact> = emptySet(),
	/** Set of all services provided to the patient during the contact. */
	@field:Valid val services: Set<Service> = emptySet(),
	/** The participants to the contact. The key is the type of participant, the value is the id of the participant data owner id */
	val participants: Map<ParticipantType, String> = emptyMap(),
	val participantList: List<ContactParticipant> = emptyList(),

	/** The secret patient key, encrypted in the patient document, in clear here. */
	override val secretForeignKeys: Set<String> = emptySet(),
	/** The public patient key, encrypted here for separate Crypto Actors. */
	override val cryptedForeignKeys: Map<String, Set<Delegation>> = emptyMap(),
	/** The delegations giving access to connected healthcare information. */
	override val delegations: Map<String, Set<Delegation>> = emptyMap(),
	/** The contact secret encryption key used to encrypt the secured properties (like services for example), encrypted for separate Crypto Actors. */
	override val encryptionKeys: Map<String, Set<Delegation>> = emptyMap(),
	/** The encrypted fields of this contact. */
	override val encryptedSelf: String? = null,
	override val securityMetadata: SecurityMetadata? = null,
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,
	val notes: List<Annotation> = emptyList(),
) : StoredICureDocument,
	HasEncryptionMetadata,
	Encryptable {

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
	override fun withTimestamps(created: Long?, modified: Long?) = when {
		created != null && modified != null -> this.copy(created = created, modified = modified)
		created != null -> this.copy(created = created)
		modified != null -> this.copy(modified = modified)
		else -> this
	}
	override fun withEncryptionMetadata(
		secretForeignKeys: Set<String>,
		cryptedForeignKeys: Map<String, Set<Delegation>>,
		delegations: Map<String, Set<Delegation>>,
		encryptionKeys: Map<String, Set<Delegation>>,
		securityMetadata: SecurityMetadata?
	) = copy(
		secretForeignKeys = secretForeignKeys,
		cryptedForeignKeys = cryptedForeignKeys,
		delegations = delegations,
		encryptionKeys = encryptionKeys,
		securityMetadata = securityMetadata
	)

	fun handleServiceIndexes(): Contact = if (services.any { it.index == null }) {
		val maxIndex = services.maxByOrNull { it.index ?: 0 }?.index ?: 0
		copy(
			services = services.mapIndexed { idx, it ->
				if (it.index == null) {
					it.copy(index = idx + maxIndex)
				} else {
					it
				}
			}.toSet(),
		)
	} else {
		this
	}
}

/**
 * Returns a copy of this service enriched with the information from the containing contact.
 */
fun Service.pimpWithContactInformation(contact: Contact): Service {
	check(contact.services.firstOrNull { it.id == this@pimpWithContactInformation.id } != null) {
		"Service ${this@pimpWithContactInformation.id} is not part of contact ${contact.id}"
	}
	val subContacts = contact.subContacts.filter { sc: SubContact ->
		sc.services
			.filter { sc2: ServiceLink -> sc2.serviceId != null }
			.any { sl: ServiceLink -> sl.serviceId == this@pimpWithContactInformation.id }
	}
	return this@pimpWithContactInformation.copy(
		contactId = contact.id,
		secretForeignKeys = contact.secretForeignKeys,
		cryptedForeignKeys = contact.cryptedForeignKeys,
		subContactIds = subContacts.mapNotNull { obj: SubContact -> obj.id }.toSet(),
		plansOfActionIds = subContacts.mapNotNull { obj: SubContact -> obj.planOfActionId }.toSet(),
		healthElementsIds = subContacts.mapNotNull { obj: SubContact -> obj.healthElementId }.toSet(),
		formIds = subContacts.mapNotNull { obj: SubContact -> obj.formId }.toSet(),
		delegations = contact.delegations,
		encryptionKeys = contact.encryptionKeys,
		author = contact.author,
		responsible = contact.responsible,
		securityMetadata = contact.securityMetadata,
	)
}
