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
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Classification(
	@param:JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	@field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	@field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	override val medicalLocationId: String? = null,
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	override val endOfLife: Long? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	val parentId: String? = null,
	val label: String = "",
	val templateId: String? = null,

	override val secretForeignKeys: Set<String> = emptySet(),
	override val cryptedForeignKeys: Map<String, Set<Delegation>> = emptyMap(),
	override val delegations: Map<String, Set<Delegation>> = emptyMap(),
	override val encryptionKeys: Map<String, Set<Delegation>> = emptyMap(),
	override val encryptedSelf: String? = null,
	override val securityMetadata: SecurityMetadata? = null,
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,

) : StoredICureDocument,
	HasEncryptionMetadata,
	Encryptable {
	companion object : DynamicInitializer<Classification>

	fun merge(other: Classification) = Classification(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: Classification) = super<StoredICureDocument>.solveConflictsWith(other) +
		super<HasEncryptionMetadata>.solveConflictsWith(other) +
		super<Encryptable>.solveConflictsWith(other) +
		mapOf(
			"parentId" to (this.parentId ?: other.parentId),
			"label" to if (this.label.isBlank()) other.label else this.label,
		)

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
	override fun withTimestamps(created: Long?, modified: Long?) = when {
		created != null && modified != null -> this.copy(created = created, modified = modified)
		created != null -> this.copy(created = created)
		modified != null -> this.copy(modified = modified)
		else -> this
	}
}
