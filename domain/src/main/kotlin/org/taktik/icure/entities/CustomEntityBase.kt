package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.icure.cardinal.entities.RawJson
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.Extendable
import org.taktik.icure.entities.base.CustomisableRoot
import org.taktik.icure.entities.base.HasDataAttachments
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.StoredICureDocument
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.DeletedAttachment
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.entities.objectstorage.DataAttachment

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CustomEntityBase(
	@param:JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,
	override val secretForeignKeys: Set<String> = emptySet(),
	override val cryptedForeignKeys: Map<String, Set<Delegation>> = emptyMap(),
	override val delegations: Map<String, Set<Delegation>> = emptyMap(),
	override val encryptionKeys: Map<String, Set<Delegation>> = emptyMap(),
	override val securityMetadata: SecurityMetadata? = null,
	override val created: Long? = null,
	override val modified: Long? = null,
	override val author: String? = null,
	override val responsible: String? = null,
	override val medicalLocationId: String? = null,
	override val endOfLife: Long? = null,
	override val tags: Set<CodeStub> = emptySet(),
	override val codes: Set<CodeStub> = emptySet(),
	override val customisedModelVersion: Int? = null,
	override val extensions: RawJson.JsonObject? = null,
	override val dataAttachments: Map<String, DataAttachment> = emptyMap(),
	override val deletedAttachments: List<DeletedAttachment> = emptyList(),
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	/**
	 * A unique identifier that is used to identify the type of custom entity.
	 * Comes with a rules baggage which depends on how the entity is configured.
	 */
	val entityTypeId: String,
) : HasEncryptionMetadata,
	StoredICureDocument,
	CustomisableRoot,
	HasDataAttachments<CustomEntityBase>,
	Extendable {
	override fun withEncryptionMetadata(
		secretForeignKeys: Set<String>,
		cryptedForeignKeys: Map<String, Set<Delegation>>,
		delegations: Map<String, Set<Delegation>>,
		encryptionKeys: Map<String, Set<Delegation>>,
		securityMetadata: SecurityMetadata?,
	): CustomEntityBase =
		copy(
			secretForeignKeys = secretForeignKeys,
			cryptedForeignKeys = cryptedForeignKeys,
			delegations = delegations,
			encryptionKeys = encryptionKeys,
			securityMetadata = securityMetadata,
		)

	override fun withDeletionDate(
		deletionDate: Long?
	): CustomEntityBase =
		copy(
			deletionDate = deletionDate
		)

	override fun withIdRev(
		id: String?,
		rev: String,
	): CustomEntityBase =
		copy(
			id = id ?: this.id,
			rev = rev
		)

	override fun withTimestamps(
		created: Long?,
		modified: Long?,
	): CustomEntityBase = copy(
		created = created,
		modified = modified
	)

	override fun withUpdatedDataAttachment(
		key: String,
		newValue: DataAttachment?,
	): CustomEntityBase =
		copy(
			dataAttachments = if (newValue != null) dataAttachments + (key to newValue) else dataAttachments - key
		)

	override fun withDataAttachments(newDataAttachments: Map<String, DataAttachment>): CustomEntityBase =
		copy(
			dataAttachments = newDataAttachments
		)

	override fun withDeletedAttachments(newDeletedAttachments: List<DeletedAttachment>): CustomEntityBase =
		copy(
			deletedAttachments = newDeletedAttachments
		)
}