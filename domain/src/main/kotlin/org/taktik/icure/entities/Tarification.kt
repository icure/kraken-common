/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.AppendixType
import org.taktik.icure.entities.base.CodeFlag
import org.taktik.icure.entities.base.CodeIdentification
import org.taktik.icure.entities.base.LinkQualification
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.LetterValue
import org.taktik.icure.entities.embed.Periodicity
import org.taktik.icure.entities.embed.PricingDomain
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.Valorisation
import org.taktik.icure.mergers.annotations.MergeStrategyUse

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Tarification(
	@param:JsonProperty("_id") override val id: String, // id = type|code|version  => this must be unique
	@param:JsonProperty("_rev") override val rev: String? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	override val context: String? = null, // ex: When embedded the context where this code is used
	override val type: String? = null, // ex: ICD (type + version + code combination must be unique) (or from tags -> CD-ITEM)
	override val code: String? = null, // ex: I06.2 (or from tags -> healthcareelement). Local codes are encoded as LOCAL:SLLOCALFROMMYSOFT
	override val version: String? = null, // ex: 10. Must be lexicographically searchable
	override val label: Map<String, String>? = null, // ex: {en: Rheumatic Aortic Stenosis, fr: Sténose rhumatoïde de l'Aorte}

	val domain: PricingDomain? = null,
	val author: String? = null,
	val regions: Set<String> = emptySet(), // ex: be,fr
	val periodicity: List<Periodicity> = emptyList(),
	val level: Int? = null, // ex: 0 = System, not to be modified by user, 1 = optional, created or modified by user
	val links: List<String> = emptyList(), // Links towards related codes (corresponds to an approximate link in qualifiedLinks)
	val qualifiedLinks: Map<LinkQualification, List<String>> = emptyMap(), // Links towards related codes
	val flags: Set<CodeFlag> = emptySet(), // flags (like female only) for the code
	@MergeStrategyUse(
		canMerge = "true",
		merge = "mergeMapsOfSets({{LEFT}}.{{PROP}}, {{RIGHT}}.{{PROP}})",
		imports = ["org.taktik.icure.entities.utils.MergeUtil.mergeMapsOfSets"]
	)
	val searchTerms: Map<String, Set<String>> = emptyMap(), // Extra search terms/ language
	val data: String? = null,
	val appendices: Map<AppendixType, String> = emptyMap(),
	val disabled: Boolean = false,
	val valorisations: Set<Valorisation> = emptySet(),
	val category: Map<String, String> = emptyMap(),
	val consultationCode: Boolean? = null,
	val hasRelatedCode: Boolean? = null,
	val needsPrescriber: Boolean? = null,
	val relatedCodes: Set<String> = emptySet(),
	@param:JsonProperty("nGroup") val ngroup: String? = null, // An obscure bug prevents Jackson to interpret the annotation if the name of the property is xAbcd
	val letterValues: List<LetterValue> = emptyList(),

	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,

	) : StoredDocument,
	CodeIdentification {
	companion object {
		fun from(type: String, code: String, version: String) = Tarification(id = "$type|$code|$version", type = type, code = code, version = version)
	}

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)

	override fun normalizeIdentification(): Tarification {
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
