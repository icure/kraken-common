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
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEndOfLifeDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.KeywordSubwordDto

@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a keyword used for indexing and searching medical data. A keyword can contain
 * sub-words to enable more flexible search matching.
 */
data class KeywordDto(
	/** The unique identifier of the keyword. */
	override val id: String,
	/** The revision of the keyword in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation. */
	override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification. */
	override val modified: Long? = null,
	/** The id of the User that created this keyword. */
	override val author: String? = null,
	/** The id of the HealthcareParty that is responsible for this keyword. */
	override val responsible: String? = null,
	/** The id of the medical location where this keyword was created. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** Tags that qualify the keyword as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this particular keyword. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	override val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** The keyword value. */
	val value: String? = null,
	/** The set of sub-words associated with this keyword. */
	val subWords: Set<KeywordSubwordDto> = emptySet(),
	/** The id of the user who owns this keyword. */
	val userId: String? = null,
) : StoredDocumentDto,
	ICureDocumentDto<String>,
	HasEndOfLifeDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
