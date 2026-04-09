/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.base

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.embed.Periodicity
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.mergers.annotations.Mergeable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["id"])
data class Code(
	/** The Id of the code, formatted as type|code|version. Must be unique. */
	@param:JsonProperty("_id") override val id: String, // id = type|code|version  => this must be unique
	/** The revision of the code in the database, used for conflict management / optimistic locking. */
	@param:JsonProperty("_rev") override val rev: String? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	/** The context in which this code is used when embedded. */
	override val context: String? = null,
	/** The code type (e.g., ICD). The type + version + code combination must be unique. */
	override val type: String? = null,
	/** The code value (e.g., I06.2). Local codes are encoded as LOCAL:SLLOCALFROMMYSOFT. */
	override val code: String? = null,
	/** The version of the code (e.g., 10). Must be lexicographically searchable. */
	override val version: String? = null,
	/** The human-readable label for the code, mapped by language (e.g., {en: "...", fr: "..."}). */
	override val label: Map<String, String>? = null, // ex: {en: Rheumatic Aortic Stenosis, fr: Sténose rhumatoïde de l'Aorte}

	/** The id of the author of this code. */
	val author: String? = null,
	/** The geographic regions where this code is applicable (e.g., be, fr). */
	val regions: Set<String> = emptySet(),
	/** The periodicities associated with this code. */
	val periodicity: Set<Periodicity> = emptySet(),
	/** The access level of the code (e.g., 0 = System, 1 = optional/user-modifiable). */
	val level: Int? = null,
	/** Links towards related codes (corresponds to approximate links in qualifiedLinks). */
	val links: Set<String> = emptySet(),
	/** Qualified links towards related codes. */
	val qualifiedLinks: Map<String, List<String>> = emptyMap(),
	/** Flags (like female only) for the code. */
	val flags: Set<CodeFlag> = emptySet(),
	/** Extra search terms indexed by language. */
	val searchTerms: Map<String, Set<String>> = emptyMap(),
	/** Additional data associated with this code. */
	val data: String? = null,
	/** Appendices associated with this code, keyed by appendix type. */
	val appendices: Map<AppendixType, String> = emptyMap(),
	/** Whether this code is disabled. */
	val disabled: Boolean = false,

	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,
) : StoredDocument,
	CodeIdentification {

	companion object {
		fun from(type: String, code: String, version: String) = Code(id = "$type|$code|$version", type = type, code = code, version = version)
	}

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)

	override fun normalizeIdentification(): Code {
		val parts = this.id.split("|").toTypedArray()
		return if (this.type == null || this.code == null || this.version == null) {
			this.copy(
				type = this.type ?: parts[0],
				code = this.code ?: parts[1],
				version = this.version ?: parts[2],
			)
		} else {
			this
		}
	}
}
