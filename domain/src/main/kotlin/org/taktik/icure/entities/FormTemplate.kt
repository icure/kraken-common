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
import org.taktik.icure.entities.base.HasTags
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.DocumentGroup
import org.taktik.icure.entities.embed.RevisionInfo

/**
 * Created by aduchate on 09/07/13, 16:27
 *
 * Note: this is not a `HasDataAttachments` entity. On the client side we don't treat this as an entity with
 * attachments: the fact we store part of its content as a couchdb attachment is an implementation detail.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FormTemplate(
	@param:JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	@JsonIgnore val templateLayout: ByteArray? = null,

	@Deprecated("Use templateLayout")
	@JsonIgnore val layout: ByteArray? = null,
	@JsonIgnore var isAttachmentDirty: Boolean = false,
	val name: String? = null,
	// Globally unique and consistent accross all DBs that get their formTemplate from a icure cloud library
	// The id is not guaranteed to be consistent accross dbs
	val guid: String? = null,
	val group: DocumentGroup? = null,
	val descr: String? = null,
	val disabled: String? = null,
	val specialty: CodeStub? = null,
	val author: String? = null,
	// Location in the form of a gpath/xpath like location with an optional action
	// ex: healthElements[codes[type == 'ICD' and code == 'I80']].plansOfAction[descr='Follow-up'] : add inside the follow-up plan of action of a specific healthElement
	// ex: healthElements[codes[type == 'ICD' and code == 'I80']].plansOfAction += [descr:'Follow-up'] : create a new planOfAction and add inside it
	val formInstancePreferredLocation: String? = null,
	val keyboardShortcut: String? = null,
	val shortReport: String? = null,
	val mediumReport: String? = null,
	val longReport: String? = null,
	val reports: Set<String> = emptySet(),

	val templateLayoutAttachmentId: String? = null,

	@Deprecated("Use templateLayoutAttachmentId")
	val layoutAttachmentId: String? = null,
	override val tags: Set<CodeStub> = emptySet(),

	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,
	// userId
) : StoredDocument,
	HasTags {

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
