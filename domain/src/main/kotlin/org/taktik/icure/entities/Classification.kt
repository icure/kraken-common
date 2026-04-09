/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.StoredICureDocument
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Encryptable
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.mergers.annotations.MergeStrategyNotBlank
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Classification(
	/** The Id of the classification. We encourage using either a v4 UUID or a HL7 Id. */
	@param:JsonProperty("_id") override val id: String,
	/** The revision of the classification in the database, used for conflict management / optimistic locking. */
	@param:JsonProperty("_rev") override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation of this entity. */
	@field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification of this entity. */
	@field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	/** The id of the User that created this classification. */
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	/** The id of the data owner that is responsible for this classification. */
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	/** The medical location where this entity was created. */
	override val medicalLocationId: String? = null,
	/** Tags that qualify the classification as being member of a certain class. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	/** Codes that identify or qualify this particular classification. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	override val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	/** The id of the parent classification, for nesting. */
	val parentId: String? = null,
	/** A human-readable label for this classification. */
	@MergeStrategyNotBlank val label: String = "",
	/** The id of the classification template this classification is based on. */
	val templateId: String? = null,

	/** The secret foreign keys, used for secure linking to patients. */
	override val secretForeignKeys: Set<String> = emptySet(),
	/** The encrypted foreign keys. */
	override val cryptedForeignKeys: Map<String, Set<Delegation>> = emptyMap(),
	/** The delegations giving access to connected healthcare information. */
	override val delegations: Map<String, Set<Delegation>> = emptyMap(),
	/** The encryption keys used to encrypt secured properties, encrypted for separate Crypto Actors. */
	override val encryptionKeys: Map<String, Set<Delegation>> = emptyMap(),
	/** The base64-encoded encrypted fields of this classification. */
	override val encryptedSelf: String? = null,
	/** The security metadata of this entity, for access control. */
	override val securityMetadata: SecurityMetadata? = null,
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,

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
}
