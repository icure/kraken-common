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
import org.taktik.icure.mergers.annotations.Mergeable
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable
data class EntityTemplate(
	@param:JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) var userId: String? = null,
	val descr: String? = null,
	val keywords: Set<String>? = null,
	val entityType: String? = null,
	val subType: String? = null,
	@param:JsonProperty("isDefaultTemplate") val defaultTemplate: Boolean? = null,
	val entity: List<JsonNode> = emptyList(),

	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,

) : StoredDocument {
	companion object : DynamicInitializer<EntityTemplate>

	fun merge(other: EntityTemplate) = EntityTemplate(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: EntityTemplate) = super.solveConflictsWith(other) +
		mapOf(
			"descr" to (this.descr ?: other.descr),
			"keywords" to ((other.keywords ?: setOf()) + (this.keywords ?: setOf())),
			"entityType" to (this.entityType ?: other.entityType),
			"subType" to (this.subType ?: other.subType),
			"defaultTemplate" to (this.defaultTemplate ?: other.defaultTemplate),
			"entity" to (this.entity),
		)

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
