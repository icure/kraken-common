/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */
package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.icure.services.external.rest.v2.dto.base.AppendixTypeDto
import org.taktik.icure.services.external.rest.v2.dto.base.CodeFlagDto
import org.taktik.icure.services.external.rest.v2.dto.base.CodeIdentificationDto
import org.taktik.icure.services.external.rest.v2.dto.base.LinkQualificationDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.LetterValueDto
import org.taktik.icure.services.external.rest.v2.dto.embed.PeriodicityDto
import org.taktik.icure.services.external.rest.v2.dto.embed.PricingDomainDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ValorisationDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a tarification code used for billing and invoicing healthcare services. Tarifications define
 * the pricing and conditions for medical acts and are identified by a combination of type, code, and version.
 */
data class TarificationDto(
	/** The unique identifier of the tarification, formatted as type|code|version. */
	override val id: String, // id = type|code|version  => this must be unique
	/** The revision of the tarification in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** The label of the tarification in different languages. */
	val label: Map<String, String>? = null, // ex: {en: Rheumatic Aortic Stenosis, fr: Sténose rhumatoïde de l'Aorte}
	/** The context where this tarification is used when embedded. */
	override val context: String? = null, // ex: When embedded the context where this code is used
	/** The type of the tarification (e.g., ICD). Type + version + code combination must be unique. */
	override val type: String? = null, // ex: ICD (type + version + code combination must be unique) (or from tags -> CD-ITEM)
	/** The code of the tarification (e.g., I06.2). */
	override val code: String? = null, // ex: I06.2 (or from tags -> healthcareelement). Local codes are encoded as LOCAL:SLLOCALFROMMYSOFT
	/** The version of the tarification. Must be lexicographically searchable. */
	override val version: String? = null, // ex: 10. Must be lexicographically searchable
	/** The pricing domain for this tarification (ambulatory, hospital, or both). */
	val domain: PricingDomainDto? = null,
	/** The author of the tarification. */
	val author: String? = null,
	/** The regions where this tarification applies (e.g., be, fr). */
	val regions: Set<String> = emptySet(), // ex: be,fr
	/** The periodicity rules for this tarification. */
	val periodicity: List<PeriodicityDto> = emptyList(),
	/** The level of the tarification (0 = system, 1 = optional/user-created). */
	val level: Int? = null, // ex: 0 = System, not to be modified by user, 1 = optional, created or modified by user
	/** Links towards related codes. */
	val links: List<String> = emptyList(), // Links towards related codes (corresponds to an approximate link in qualifiedLinks)
	/** Qualified links towards related codes. */
	val qualifiedLinks: Map<LinkQualificationDto, List<String>> = emptyMap(), // Links towards related codes
	/** Flags for the tarification (e.g., female only). */
	val flags: Set<CodeFlagDto> = emptySet(), // flags (like female only) for the code
	/** Extra search terms per language. */
	val searchTerms: Map<String, Set<String>> = emptyMap(), // Extra search terms/ language
	/** Additional data associated with the tarification. */
	val data: String? = null,
	/** Map of appendix types to their content. */
	val appendices: Map<AppendixTypeDto, String> = emptyMap(),
	/** Whether this tarification is disabled. */
	val disabled: Boolean = false,
	/** The set of valorisations (pricing) for this tarification. */
	val valorisations: Set<ValorisationDto> = emptySet(),
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
	@param:JsonProperty("nGroup")
	val ngroup: String? = null,
	/** The list of letter values for this tarification. */
	val letterValues: List<LetterValueDto> = emptyList(),
) : StoredDocumentDto,
	CodeIdentificationDto<String> {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)

	override fun normalizeIdentification(): TarificationDto {
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
