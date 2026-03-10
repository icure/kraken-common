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
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.SdkNonNullable
import org.taktik.icure.services.external.rest.v2.dto.base.AppendixTypeDto
import org.taktik.icure.services.external.rest.v2.dto.base.CodeFlagDto
import org.taktik.icure.services.external.rest.v2.dto.base.CodeIdentificationDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.PeriodicityDto

@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a medical code from a terminology system (e.g., ICD, SNOMED). The code id is composed of
 * type|code|version and must be unique.
 */
data class CodeDto(
	/** The Id of the code, formatted as type|code|version. Must be unique. */
	override val id: String, // id = type|code|version  => this must be unique
	/** The revision of the code in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** The context in which this code is used when embedded. */
	override val context: String? = null, // ex: When embedded the context where this code is used
	/** The code type (e.g., ICD). The type + version + code combination must be unique. */
	override val type: String? = null, // ex: ICD (type + version + code combination must be unique) (or from tags -> CD-ITEM)
	/** The code value (e.g., I06.2). Local codes are encoded as LOCAL:SLLOCALFROMMYSOFT. */
	override val code: String? = null, // ex: I06.2 (or from tags -> healthcareelement). Local codes are encoded as LOCAL:SLLOCALFROMMYSOFT
	/** The version of the code (e.g., 10). Must be lexicographically searchable. */
	override val version: String? = null, // ex: 10. Must be lexicographically searchable
	/** The human-readable label for the code, mapped by language (e.g., {en: "...", fr: "..."}). */
	@SdkNonNullable @param:JsonInclude(JsonInclude.Include.NON_NULL) val label: Map<String, String>? = null, // ex: {en: Rheumatic Aortic Stenosis, fr: Sténose rhumatoïde de l'Aorte}
	/** The id of the author of this code. */
	val author: String? = null,
	/** The geographic regions where this code is applicable (e.g., be, fr). */
	val regions: Set<String> = emptySet(), // ex: be,fr
	/** The periodicities associated with this code. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val periodicity: Set<PeriodicityDto> = emptySet(),
	/** The access level of the code (e.g., 0 = System, 1 = optional/user-modifiable). */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val level: Int? = null, // ex: 0 = System, not to be modified by userDto, 1 = optional, created or modified by userDto
	/** Links towards related codes (corresponds to approximate links in qualifiedLinks). */
	val links: Set<String> = emptySet(), // Links towards related codes (corresponds to an approximate link in qualifiedLinks)
	/** Qualified links towards related codes. */
	val qualifiedLinks: Map<String, List<String>> = emptyMap(), // Links towards related codes
	/** Flags (like female only) for the code. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val flags: Set<CodeFlagDto> = emptySet(), // flags (like female only) for the code
	/** Extra search terms indexed by language. */
	val searchTerms: Map<String, Set<String>> = emptyMap(), // Extra search terms/ language
	/** Additional data associated with this code. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val data: String? = null,
	/** Appendices associated with this code, keyed by appendix type. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val appendices: Map<AppendixTypeDto, String> = emptyMap(),
	/** Whether this code is disabled. */
	@param:Schema(defaultValue = "false") val disabled: Boolean = false,
) : StoredDocumentDto,
	CodeIdentificationDto<String> {
	companion object {
		fun from(
			type: String,
			code: String,
			version: String,
		) = CodeDto(id = "$type|$code|$version", type = type, code = code, version = version)
	}

	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)

	override fun normalizeIdentification(): CodeDto {
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
