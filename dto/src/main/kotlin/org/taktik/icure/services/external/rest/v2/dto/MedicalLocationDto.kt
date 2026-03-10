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
import org.taktik.icure.services.external.rest.v2.dto.base.NamedDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AddressDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a medical location such as a clinic, office, or hospital ward where healthcare services are provided.
 */
data class MedicalLocationDto(
	/** The unique identifier of the medical location. */
	override val id: String,
	/** The revision of the medical location in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** The name of the medical location. */
	override val name: String? = null,
	/** A description of the medical location. */
	val description: String? = null,
	/** The id of the healthcare party responsible for this medical location. */
	val responsible: String? = null,
	/** Whether this medical location is a guard post. */
	val guardPost: Boolean? = null,
	/** The CBE (Crossroads Bank for Enterprises) number of the medical location. */
	val cbe: String? = null,
	/** The Bank Identifier Code (BIC/SWIFT) of the medical location. */
	val bic: String? = null,
	/** The bank account number (IBAN) of the medical location. */
	val bankAccount: String? = null,
	/** The NIHII number of the medical location. */
	val nihii: String? = null,
	/** The social security inscription number associated with the medical location. */
	val ssin: String? = null,
	/** The address of the medical location. */
	val address: AddressDto? = null,
	/** The set of agenda ids linked to this medical location. */
	val agendaIds: Set<String> = emptySet(),
	/** Additional options for the medical location. */
	val options: Map<String, String> = emptyMap(),
	/** Public information about the medical location, in multiple languages. */
	val publicInformations: Map<String, String> = emptyMap(),
) : StoredDocumentDto,
	NamedDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
