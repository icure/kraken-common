/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.StoredICureDocument
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.mergers.annotations.MergeStrategyNotBlank
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ClassificationTemplate(
	/** The Id of the classification template. */
	@param:JsonProperty("_id") override val id: String,
	/** The revision of the classification template in the database, used for conflict management / optimistic locking. */
	@param:JsonProperty("_rev") override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation of this entity. */
	@field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification of this entity. */
	@field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	/** The id of the User that created this classification template. */
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	/** The id of the data owner that is responsible for this classification template. */
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	/** The medical location where this entity was created. */
	override val medicalLocationId: String? = null,
	/** Tags that qualify the classification template as being member of a certain class. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	/** Codes that identify or qualify this particular classification template. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	override val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	/** The id of the parent classification template, for nesting. */
	val parentId: String? = null,
	/** A human-readable label for this classification template. */
	@MergeStrategyNotBlank val label: String = "",

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
