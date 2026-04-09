package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.PropertyStub
import org.taktik.icure.entities.base.StoredICureDocument
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Encryptable
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.entities.embed.TaskStatus
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MaintenanceTask(
	/** The unique identifier of the maintenance task. */
	@param:JsonProperty("_id") override val id: String,
	/** The revision of the maintenance task in the database, used for conflict management / optimistic locking. */
	@param:JsonProperty("_rev") override val rev: String? = null,
	/** The identifiers of the maintenance task. */
	val identifier: List<Identifier> = listOf(),
	/** The id of the medical location where this maintenance task was created. */
	override val medicalLocationId: String? = null,
	/** The timestamp (unix epoch in ms) of creation. */
	@field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification. */
	@field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	/** The id of the User that created this maintenance task. */
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	/** The id of the HealthcareParty that is responsible for this maintenance task. */
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	/** Tags that qualify the maintenance task as being member of a certain class. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	/** Codes that identify or qualify this particular maintenance task. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	override val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	/** The type of the maintenance task. */
	val taskType: String? = null,
	/** Extra properties for the maintenance task. */
	val properties: Set<PropertyStub> = emptySet(),
	/** The current status of the maintenance task (pending, ongoing, cancelled, completed). */
	val status: TaskStatus = TaskStatus.pending,

	/** The secret patient key, encrypted in the patient's own AES key. */
	override val secretForeignKeys: Set<String> = emptySet(),
	/** The patient id encrypted in the delegates' AES keys. */
	override val cryptedForeignKeys: Map<String, Set<Delegation>> = emptyMap(),
	/** The delegations giving access to connected healthcare information. */
	override val delegations: Map<String, Set<Delegation>> = emptyMap(),
	/** The keys used to encrypt this entity when stored encrypted. */
	override val encryptionKeys: Map<String, Set<Delegation>> = emptyMap(),
	/** The base64-encoded encrypted fields of this entity. */
	override val encryptedSelf: String? = null,
	/** The security metadata of the entity. */
	override val securityMetadata: SecurityMetadata? = null,

	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,

) : StoredICureDocument,
	HasEncryptionMetadata,
	Encryptable {
	override fun withTimestamps(created: Long?, modified: Long?) = when {
		created != null && modified != null -> this.copy(created = created, modified = modified)
		created != null -> this.copy(created = created)
		modified != null -> this.copy(modified = modified)
		else -> this
	}
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
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
