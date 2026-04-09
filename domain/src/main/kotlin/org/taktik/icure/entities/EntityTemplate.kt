/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EntityTemplate(
	/** The Id of the entity template. */
	@param:JsonProperty("_id") override val id: String,
	/** The revision of the entity template in the database, used for conflict management / optimistic locking. */
	@param:JsonProperty("_rev") override val rev: String? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	/** A description of the entity template. */
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) var userId: String? = null,
	val descr: String? = null,
	/** A set of keywords for searching and categorizing the template. */
	val keywords: Set<String>? = null,
	/** The type of entity this template is for. */
	val entityType: String? = null,
	/** The sub-type of entity this template is for. */
	val subType: String? = null,
	/** Whether this is the default template for its entity type and sub-type. */
	@param:JsonProperty("isDefaultTemplate") val defaultTemplate: Boolean? = null,
	/** The JSON representation of the template entity content. */
	val entity: List<JsonNode> = emptyList(),

	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,

) : StoredDocument {

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
