/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.ReportVersion
import org.taktik.icure.entities.base.StoredICureDocument
import org.taktik.icure.entities.embed.DocumentGroup
import org.taktik.icure.entities.embed.DocumentType
import org.taktik.icure.entities.embed.RevisionInfo
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
	/** The Id of the document template. */
	@param:JsonProperty("_id") override val id: String,
	/** The revision of the document template in the database, used for conflict management / optimistic locking. */
	@param:JsonProperty("_rev") override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation of this entity. */
	@field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification of this entity. */
	@field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	/** The id of the User that created this document template. */
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	/** The id of the data owner that is responsible for this document template. */
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	/** The medical location where this entity was created. */
	override val medicalLocationId: String? = null,
	/** Tags that qualify the document template as being member of a certain class. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	/** Codes that identify or qualify this particular document template. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	override val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	/** The binary content of the template attachment. */
	@JsonIgnore val attachment: ByteArray? = null,
	/** The main Uniform Type Identifier of the template attachment. */
	@JsonIgnore var isAttachmentDirty: Boolean = false,
	val mainUti: String? = null,
	/** The name of the document template. */
	val name: String? = null,
	/** Extra Uniform Type Identifiers for the template attachment. */
	val otherUtis: Set<String> = emptySet(),
	/** The id of the CouchDB attachment for the template. */
	val attachmentId: String? = null,
	/** The version of the report template. */
	val version: ReportVersion? = null,
	/** The owner of the document template. */
	val owner: String? = null,
	/** A globally unique identifier for the template. */
	val guid: String? = null,
	/** The document group this template belongs to. */
	val group: DocumentGroup? = null,
	/** A description of the document template. */
	val descr: String? = null,
	/** Whether this template is disabled. */
	val disabled: String? = null,
	/** The medical specialty associated with this template. */
	val specialty: CodeStub? = null,
	/** The type of document (e.g., admission, clinical path, document report, invoice). */
	val documentType: DocumentType? = null,

	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,

) : StoredICureDocument {

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
	override fun withTimestamps(created: Long?, modified: Long?) = when {
		created != null && modified != null -> this.copy(created = created, modified = modified)
		created != null -> this.copy(created = created)
		modified != null -> this.copy(modified = modified)
		else -> this
	}
}
