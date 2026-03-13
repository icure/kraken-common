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
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents employment information for a patient, including the period, profession type, and employer details.
 */
data class EmploymentInfoDto(
	/** The start date (unix epoch in ms) of the employment. */
	val startDate: Long? = null,
	/** The end date (unix epoch in ms) of the employment. */
	val endDate: Long? = null,
	/** A code describing the profession type. */
	val professionType: CodeStubDto? = null,
	/** The employer details. */
	val employer: EmployerDto? = null,
	/** The base64-encoded encrypted content of this employment info. */
	override val encryptedSelf: Base64StringDto? = null,
) : Serializable,
	EncryptableDto
