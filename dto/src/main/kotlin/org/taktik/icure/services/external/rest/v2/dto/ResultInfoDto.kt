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

import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ServiceDto

/**
 * DTO containing the result information from a lab or diagnostic report, including patient
 * identification, protocol data, and associated services.
 */
class ResultInfoDto(
	/** The social security identification number of the patient. */
	val ssin: String? = null,
	/** The last name of the patient. */
	val lastName: String? = null,
	/** The first name of the patient. */
	val firstName: String? = null,
	/** The date of birth of the patient as a fuzzy date (YYYYMMDD format). */
	val dateOfBirth: Long? = null,
	/** The sex of the patient. */
	val sex: String? = null,
	/** The identifier of the document associated with this result. */
	val documentId: String? = null,
	/** The protocol identifier for this result. */
	val protocol: String? = null,
	/** Whether the result is complete. */
	val complete: Boolean? = null,
	/** The date when the analysis was requested, in epoch milliseconds. */
	val demandDate: Long? = null,
	/** The identifier or name of the laboratory that produced the result. */
	val labo: String? = null,
	/** The engine or system used to produce the result. */
	val engine: String? = null,
	/** The set of codes associated with this result. */
	val codes: Set<CodeStubDto> = emptySet(),
	/** The list of services contained in this result. */
	val services: List<ServiceDto>? = null,
)
