/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.ReportVersion
import org.taktik.icure.entities.base.StoredICureDocument
import org.taktik.icure.entities.embed.DocumentGroup
import org.taktik.icure.entities.embed.DocumentType
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode

/**
 * Note: this is not a `HasDataAttachments` entity. On the client side we don't treat this as an entity with
 * attachments: the fact we store part of its content as a couchdb attachment is an implementation detail.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DocumentTemplate(
	@param:ContentValue(ContentValues.UUID) @JsonProperty("_id") override val id: String,
	@JsonProperty("_rev") override val rev: String? = null,
	@field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	@field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	override val medicalLocationId: String? = null,
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	override val endOfLife: Long? = null,
	@JsonProperty("deleted") override val deletionDate: Long? = null,

	@JsonIgnore val attachment: ByteArray? = null,
	@JsonIgnore var isAttachmentDirty: Boolean = false,
	val mainUti: String? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val name: String? = null,
	val otherUtis: Set<String> = emptySet(),
	val attachmentId: String? = null,
	val version: ReportVersion? = null,
	val owner: String? = null,
	val guid: String? = null,
	@param:ContentValue(ContentValues.NESTED_ENTITY) val group: DocumentGroup? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val descr: String? = null,
	val disabled: String? = null,
	val specialty: CodeStub? = null,
	val documentType: DocumentType? = null,

	@JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,

) : StoredICureDocument {
	companion object : DynamicInitializer<DocumentTemplate>

	fun merge(other: DocumentTemplate) = DocumentTemplate(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: DocumentTemplate) = super<StoredICureDocument>.solveConflictsWith(other) +
		mapOf(
			"mainUti" to (this.mainUti ?: other.mainUti),
			"name" to (this.name ?: other.name),
			"otherUtis" to (other.otherUtis + this.otherUtis),
			"attachmentId" to (this.attachmentId ?: other.attachmentId),
			"version" to (this.version ?: other.version),
			"owner" to (this.owner ?: other.owner),
			"guid" to (this.guid ?: other.guid),
			"group" to (this.group ?: other.group),
			"descr" to (this.descr ?: other.descr),
			"disabled" to (this.disabled ?: other.disabled),
			"specialty" to (this.specialty ?: other.specialty),
			"attachment" to (
				this.attachment?.let { if (it.size >= (other.attachment?.size ?: 0)) it else other.attachment }
					?: other.attachment
				),
			"documentType" to (this.documentType ?: other.documentType),
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
