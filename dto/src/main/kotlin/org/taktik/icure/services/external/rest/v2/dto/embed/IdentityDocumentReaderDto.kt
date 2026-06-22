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

package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents information about how an identity document (such as a Belgian eID) was read,
 * including the document number, support serial number, and encoding details.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.embed.IdentityDocumentReaderDto")
data class IdentityDocumentReaderDto(
	/** The justification document number. */
	@ActiveField val justificatifDocumentNumber: String? = null,
	/** The serial number of the support used to read the document. */
	@ActiveField val supportSerialNumber: String? = null,
	/** The timestamp (unix epoch in ms) when the eID document was read. */
	@ActiveField val timeReadingEIdDocument: Long? = null,
	/** The type of eID document support used. */
	@param:Schema(defaultValue = "0") @ActiveField val eidDocumentSupportType: Int = 0,
	/** The reason code for manual encoding, if applicable. */
	@param:Schema(defaultValue = "0") @ActiveField val reasonManualEncoding: Int = 0,
	/** The reason code for using a vignette, if applicable. */
	@param:Schema(defaultValue = "0") @ActiveField val reasonUsingVignette: Int = 0,
) : Serializable
