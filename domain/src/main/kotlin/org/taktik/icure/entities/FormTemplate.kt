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
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke

/**
 * Created by aduchate on 09/07/13, 16:27
 *
 * Note: this is not a `HasDataAttachments` entity. On the client side we don't treat this as an entity with
 * attachments: the fact we store part of its content as a couchdb attachment is an implementation detail.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FormTemplate(
	@JsonProperty("_id") override val id: String,
	@JsonProperty("_rev") override val rev: String? = null,
	@JsonProperty("deleted") override val deletionDate: Long? = null,

	@JsonIgnore val templateLayout: ByteArray? = null,

	@Deprecated("Use templateLayout")
	@JsonIgnore val layout: ByteArray? = null,
	@JsonIgnore var isAttachmentDirty: Boolean = false,
	val name: String? = null,
	//Globally unique and consistent accross all DBs that get their formTemplate from a icure cloud library
	//The id is not guaranteed to be consistent accross dbs
	val guid: String? = null,
	val group: DocumentGroup? = null,
	val descr: String? = null,
	val disabled: String? = null,
	val specialty: CodeStub? = null,
	val author: String? = null,
	//Location in the form of a gpath/xpath like location with an optional action
	//ex: healthElements[codes[type == 'ICD' and code == 'I80']].plansOfAction[descr='Follow-up'] : add inside the follow-up plan of action of a specific healthElement
	//ex: healthElements[codes[type == 'ICD' and code == 'I80']].plansOfAction += [descr:'Follow-up'] : create a new planOfAction and add inside it
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

	@JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@JsonProperty("rev_history") override val revHistory: Map<String, String>? = null
	//userId
) : StoredDocument, HasTags {
	companion object : DynamicInitializer<FormTemplate>

	fun merge(other: FormTemplate) = FormTemplate(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: FormTemplate) = super.solveConflictsWith(other) + mapOf(
		"name" to (this.name ?: other.name),
		"guid" to (this.guid ?: other.guid),
		"group" to (this.group ?: other.group),
		"descr" to (this.descr ?: other.descr),
		"disabled" to (this.disabled ?: other.disabled),
		"specialty" to (this.specialty ?: other.specialty),
		"author" to (this.author ?: other.author),
		"formInstancePreferredLocation" to (
			this.formInstancePreferredLocation
				?: other.formInstancePreferredLocation
			),
		"keyboardShortcut" to (this.keyboardShortcut ?: other.keyboardShortcut),
		"shortReport" to (this.shortReport ?: other.shortReport),
		"mediumReport" to (this.mediumReport ?: other.mediumReport),
		"longReport" to (this.longReport ?: other.longReport),
		"reports" to (other.reports + this.reports),
		"layoutAttachmentId" to (this.layoutAttachmentId ?: other.layoutAttachmentId),
		"templateLayoutAttachmentId" to (this.templateLayoutAttachmentId ?: other.templateLayoutAttachmentId),
		"layout" to (
			this.layout?.let { if (it.size >= other.layout?.size ?: 0) it else other.layout }
				?: other.layout
			),
		"templateLayout" to (
			this.templateLayout?.let { if (it.size >= other.templateLayout?.size ?: 0) it else other.templateLayout }
				?: other.templateLayout
			)
	)

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
