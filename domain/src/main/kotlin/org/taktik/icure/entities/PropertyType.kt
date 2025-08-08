/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.constants.PropertyTypeScope
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.TypedValuesType
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PropertyType(
	@param:JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	val identifier: String,
	val type: TypedValuesType? = null,
	val scope: PropertyTypeScope? = null,
	val unique: Boolean = false,
	val editor: String? = null,
	val localized: Boolean = false,

	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,

) : StoredDocument {
	companion object : DynamicInitializer<PropertyType> {
		fun with(type: TypedValuesType, scope: PropertyTypeScope, identifier: String) = PropertyType(id = identifier, type = type, scope = scope, identifier = identifier)
	}

	fun merge(other: PropertyType) = PropertyType(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: PropertyType) = super.solveConflictsWith(other) +
		mapOf(
			"identifier" to this.identifier,
			"type" to (this.type ?: other.type),
			"scope" to (this.scope ?: other.scope),
			"unique" to (this.unique),
			"editor" to (this.editor ?: other.editor),
			"localized" to (this.localized),
		)

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
