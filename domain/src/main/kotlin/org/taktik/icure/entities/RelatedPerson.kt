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
import org.taktik.icure.entities.base.HasIdentifier
import org.taktik.icure.entities.base.Person
import org.taktik.icure.entities.base.PropertyStub
import org.taktik.icure.entities.base.StoredICureDocument
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Encryptable
import org.taktik.icure.entities.embed.Gender
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.PersonName
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.mergers.annotations.Mergeable
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode

/**
 * A person related to one or more patients, that is neither a [Patient] nor a [HealthcareParty]:
 * typically a contact person (parent of a child patient, caregiver, ...), referenced from
 * [org.taktik.icure.entities.embed.Partnership.partnerId] with
 * [org.taktik.icure.entities.embed.Partnership.partnerType] = relatedPerson.
 *
 * This entity is a root level object serialized in JSON and saved in the patient CouchDB database.
 * It is a standalone encryptable entity (it has its own encryption metadata and is shared
 * explicitly with other data owners) but it is NOT a crypto actor nor a data owner.
 *
 * @property id the Id of the related person. We encourage using either a v4 UUID or a HL7 Id.
 * @property rev the revision of the related person in the database, used for conflict management / optimistic locking.
 * @property identifier identifiers that identify uniquely and unambiguously this related person.
 * @property created the timestamp (unix epoch in ms) of creation of this entity, will be filled automatically if missing. Not enforced by the application server.
 * @property modified the date (unix epoch in ms) of the latest modification of this entity, will be filled automatically if missing. Not enforced by the application server.
 * @property author the id of the User that has created this entity, will be filled automatically if missing. Not enforced by the application server.
 * @property responsible the id of the data owner that is responsible for this entity, will be filled automatically if missing. Not enforced by the application server.
 * @property tags tags that qualify the related person as a member of a certain class.
 * @property codes codes that identify or qualify this related person.
 * @property endOfLife soft delete (unix epoch in ms) timestamp of the object.
 * @property deletionDate hard delete (unix epoch in ms) timestamp of the object.
 * @property firstName the firstname (name) of the related person.
 * @property lastName the lastname (surname) of the related person.
 * @property names the list of all names of the related person, also containing the official full name information.
 * @property companyName the name of the company this related person is member of.
 * @property languages the list of languages spoken by the related person, in ISO 639-2 alpha-2 code.
 * @property addresses the addresses and telecoms of the related person.
 * @property civility mr., ms., pr., dr. ...
 * @property gender the gender of the related person.
 * @property properties extra properties of the related person.
 * @property encryptedSelf the encrypted fields of this related person.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["id"])
data class RelatedPerson(
	@param:JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	override val identifier: List<Identifier> = emptyList(),
	override val created: Long? = null,
	override val modified: Long? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	override val endOfLife: Long? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	override val firstName: String? = null,
	override val lastName: String? = null,
	override val names: List<PersonName> = emptyList(),
	override val companyName: String? = null,
	override val languages: List<String> = emptyList(),
	@field:Valid override val addresses: List<Address> = emptyList(),
	override val civility: String? = null,
	override val gender: Gender? = Gender.unknown,
	val properties: Set<PropertyStub> = emptySet(),

	override val secretForeignKeys: Set<String> = emptySet(),
	override val cryptedForeignKeys: Map<String, Set<Delegation>> = emptyMap(),
	override val delegations: Map<String, Set<Delegation>> = emptyMap(),
	override val encryptionKeys: Map<String, Set<Delegation>> = emptyMap(),
	override val encryptedSelf: String? = null,
	override val securityMetadata: SecurityMetadata? = null,
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
) : StoredICureDocument,
	Person,
	HasEncryptionMetadata,
	HasIdentifier,
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
}
