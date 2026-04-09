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
	/** The unique identifier of the tarification, formatted as type|code|version. */
	@param:JsonProperty("_id") override val id: String, // id = type|code|version  => this must be unique
	/** The revision of the tarification in the database, used for conflict management / optimistic locking. */
	@param:JsonProperty("_rev") override val rev: String? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	/** The context where this tarification is used when embedded. */
	override val context: String? = null, // ex: When embedded the context where this code is used
	/** The type of the tarification (e.g., ICD). Type + version + code combination must be unique. */
	override val type: String? = null, // ex: ICD (type + version + code combination must be unique) (or from tags -> CD-ITEM)
	/** The code of the tarification (e.g., I06.2). */
	override val code: String? = null, // ex: I06.2 (or from tags -> healthcareelement). Local codes are encoded as LOCAL:SLLOCALFROMMYSOFT
	/** The version of the tarification. Must be lexicographically searchable. */
	override val version: String? = null, // ex: 10. Must be lexicographically searchable
	/** The label of the tarification in different languages. */
	override val label: Map<String, String>? = null, // ex: {en: Rheumatic Aortic Stenosis, fr: Sténose rhumatoïde de l'Aorte}

	/** The pricing domain for this tarification (ambulatory, hospital, or both). */
	val domain: PricingDomain? = null,
	/** The author of the tarification. */
	val author: String? = null,
	/** The regions where this tarification applies (e.g., be, fr). */
	val regions: Set<String> = emptySet(), // ex: be,fr
	/** The periodicity rules for this tarification. */
	val periodicity: List<Periodicity> = emptyList(),
	/** The level of the tarification (0 = system, 1 = optional/user-created). */
	val level: Int? = null, // ex: 0 = System, not to be modified by user, 1 = optional, created or modified by user
	/** Links towards related codes. */
	val links: List<String> = emptyList(), // Links towards related codes (corresponds to an approximate link in qualifiedLinks)
	/** Qualified links towards related codes. */
	val qualifiedLinks: Map<LinkQualification, List<String>> = emptyMap(), // Links towards related codes
	/** Flags for the tarification (e.g., female only). */
	val flags: Set<CodeFlag> = emptySet(), // flags (like female only) for the code
	/** Extra search terms per language. */
	@MergeStrategyUse(
		canMerge = "true",
		merge = "mergeMapsOfSets({{LEFT}}.{{PROP}}, {{RIGHT}}.{{PROP}})",
		imports = ["org.taktik.icure.entities.utils.MergeUtil.mergeMapsOfSets"]
	)
	val searchTerms: Map<String, Set<String>> = emptyMap(), // Extra search terms/ language
	/** Additional data associated with the tarification. */
	val data: String? = null,
	/** Map of appendix types to their content. */
	val appendices: Map<AppendixType, String> = emptyMap(),
	/** Whether this tarification is disabled. */
	val disabled: Boolean = false,
	/** The set of valorisations (pricing) for this tarification. */
	val valorisations: Set<Valorisation> = emptySet(),
	/** The category of the tarification in different languages. */
	val category: Map<String, String> = emptyMap(),
	/** Whether this is a consultation code. */
	val consultationCode: Boolean? = null,
	/** Whether this tarification has a related code. */
	val hasRelatedCode: Boolean? = null,
	/** Whether this tarification needs a prescriber. */
	val needsPrescriber: Boolean? = null,
	/** The set of related tarification codes. */
	val relatedCodes: Set<String> = emptySet(),
	/** The nGroup identifier for this tarification. */
	@param:JsonProperty("nGroup") val ngroup: String? = null, // An obscure bug prevents Jackson to interpret the annotation if the name of the property is xAbcd
	/** The list of letter values for this tarification. */
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
