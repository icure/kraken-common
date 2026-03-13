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

package org.taktik.icure.services.external.rest.v2.dto.base

import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.taktik.icure.RequireHashable

@JsonIgnoreProperties(ignoreUnknown = true)
@RequireHashable
@JsonFilter("codeStubFilter")
/**
 * A lightweight reference to a code from a codification system, used as a stub in entities that reference codes
 * without embedding the full code definition. The id is typically formatted as type|code|version.
 */
data class CodeStubDto(
	/** The unique identifier, formatted as type|code|version. */
	override val id: String? = null, // id = type|code|version  => this must be unique
	/** The context where this code is used when embedded in another entity. */
	override val context: String? = null, // ex: When embedded the context where this code is used
	/** The codification system type (e.g. ICD, ICPC-2). */
	override val type: String? = null, // ex: ICD (type + version + code combination must be unique) (or from tags -> CD-ITEM)
	/** The code value within the codification system. */
	override val code: String? = null, // ex: I06.2 (or from tags -> healthcareelement). Local codes are encoded as LOCAL:SLLOCALFROMMYSOFT
	/** The version of the codification system. */
	override val version: String? = null, // ex: 10. Must be lexicographically searchable
	/** A human-readable label for the context. */
	val contextLabel: String? = null,
	/** A map of language codes to localized labels for this code. */
	@Deprecated("label shouldn't be included in code stub but only in full codes")
	val label: Map<String, String>? = null, // ex: {en: Rheumatic Aortic Stenosis, fr: Sténose rhumatoïde de l'Aorte}
) : CodeIdentificationDto<String?> {

	override fun normalizeIdentification(): CodeStubDto {
		val parts = this.id?.split("|")?.toTypedArray()
		return if ((this.type == null || this.code == null || this.version == null) && parts != null) {
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
