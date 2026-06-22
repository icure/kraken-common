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
import org.taktik.icure.services.external.rest.v2.dto.base.MimeAttachmentDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * DTO containing the results of a data import operation, including all the entities that were
 * imported and any warnings or errors encountered during the process.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.ImportResultDto")
data class ImportResultDto(
	/** The patient that was imported or matched during the import. */
	@ActiveField val patient: PatientDto? = null,
	/** The list of health elements imported. */
	@ActiveField val hes: List<HealthElementDto> = listOf(),
	/** The list of contacts imported. */
	@ActiveField val ctcs: List<ContactDto> = listOf(),
	/** The list of warning messages generated during import. */
	@ActiveField val warnings: List<String> = listOf(),
	/** The list of error messages generated during import. */
	@ActiveField val errors: List<String> = listOf(),
	/** The list of forms imported. */
	@ActiveField val forms: List<FormDto> = listOf(),
	/** The list of healthcare parties imported. */
	@ActiveField val hcps: List<HealthcarePartyDto> = listOf(),
	/** The list of documents imported. */
	@ActiveField val documents: List<DocumentDto> = listOf(),
	/** A map of MIME attachments associated with the import, keyed by attachment identifier. */
	@ActiveField val attachments: Map<String, MimeAttachmentDto> = mapOf(),
)
