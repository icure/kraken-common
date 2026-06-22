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
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AddressDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents an insurance entity. An insurance can be a mutual fund, a private insurance company,
 * or any other type of insurance organization that covers healthcare costs.
 */
data class InsuranceDto(
	/** The unique identifier of the insurance. */
	override val id: String,
	/** The revision of the insurance in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** The name of the insurance in different languages. */
	@ActiveField val name: Map<String, String> = emptyMap(),
	/** Whether this is a private insurance. */
	@ActiveField val privateInsurance: Boolean = false,
	/** Whether this insurance covers hospitalisation. */
	@ActiveField val hospitalisationInsurance: Boolean = false,
	/** Whether this insurance covers ambulatory care. */
	@ActiveField val ambulatoryInsurance: Boolean = false,
	/** The insurance code. */
	@ActiveField val code: String? = null,
	/** The agreement number for the insurance. */
	@ActiveField val agreementNumber: String? = null,
	/** The id of the parent insurance entity. */
	@ActiveField val parent: String? = null, // ID of the parent
	/** The address of the insurance company. */
	@ActiveField val address: AddressDto = AddressDto(),
) : StoredDocumentDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
