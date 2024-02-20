/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.base

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import org.taktik.icure.entities.embed.Periodicity
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.utils.MergeUtil
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Code(
	@param:ContentValue(ContentValues.CODE_ID) @JsonProperty("_id") override val id: String, // id = type|code|version  => this must be unique
	@JsonProperty("_rev") override val rev: String? = null,
	@JsonProperty("deleted") override val deletionDate: Long? = null,

	@param:ContentValue(ContentValues.ANY_STRING) override val context: String? = null, //ex: When embedded the context where this code is used
	@param:ContentValue(ContentValues.ANY_STRING) override val type: String? = null, //ex: ICD (type + version + code combination must be unique) (or from tags -> CD-ITEM)
	@param:ContentValue(ContentValues.ANY_STRING) override val code: String? = null, //ex: I06.2 (or from tags -> healthcareelement). Local codes are encoded as LOCAL:SLLOCALFROMMYSOFT
	@param:ContentValue(ContentValues.ANY_STRING) override val version: String? = null, //ex: 10. Must be lexicographically searchable
	override val label: Map<String, String>? = null, //ex: {en: Rheumatic Aortic Stenosis, fr: Sténose rhumatoïde de l'Aorte}

	val author: String? = null,
	val regions: Set<String> = emptySet(), //ex: be,fr
	val periodicity: Set<Periodicity> = emptySet(),
	val level: Int? = null, //ex: 0 = System, not to be modified by user, 1 = optional, created or modified by user
	val links: Set<String> = emptySet(), //Links towards related codes (corresponds to an approximate link in qualifiedLinks)
	val qualifiedLinks: Map<String, List<String>> = emptyMap(), //Links towards related codes
	val flags: Set<CodeFlag> = emptySet(), //flags (like female only) for the code
	val searchTerms: Map<String, Set<String>> = emptyMap(), //Extra search terms/ language
	val data: String? = null,
	val appendices: Map<AppendixType, String> = emptyMap(),
	val disabled: Boolean = false,

	@JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@JsonProperty("rev_history") override val revHistory: Map<String, String>? = null

) : StoredDocument, CodeIdentification {
	companion object : DynamicInitializer<Code> {
		fun from(type: String, code: String, version: String) = Code(id = "$type|$code|$version", type = type, code = code, version = version)
	}

	fun merge(other: Code) = Code(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: Code) = super<StoredDocument>.solveConflictsWith(other) + super<CodeIdentification>.solveConflictsWith(other) + mapOf(
		"author" to (this.author ?: other.author),
		"regions" to (other.regions + this.regions),
		"periodicity" to (other.periodicity + this.periodicity),
		"level" to (this.level ?: other.level),
		"links" to (other.links + this.links),
		"qualifiedLinks" to (other.qualifiedLinks + this.qualifiedLinks),
		"flags" to (other.flags + this.flags),
		"searchTerms" to MergeUtil.mergeMapsOfSets(this.searchTerms, other.searchTerms),
		"data" to (this.data ?: other.data),
		"appendices" to (other.appendices + this.appendices),
		"disabled" to (this.disabled)
	)

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)

	override fun normalizeIdentification(): Code {
		val parts = this.id.split("|").toTypedArray()
		return if (this.type == null || this.code == null || this.version == null) this.copy(
			type = this.type ?: parts[0],
			code = this.code ?: parts[1],
			version = this.version ?: parts[2]
		) else this
	}
}
