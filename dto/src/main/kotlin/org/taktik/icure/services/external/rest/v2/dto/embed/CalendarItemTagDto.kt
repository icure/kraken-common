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
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a tag associated with a calendar item, carrying metadata about who tagged it and when.
 */
data class CalendarItemTagDto(
	/** The code identifying this tag. */
	val code: String? = null,
	/** The timestamp (unix epoch in ms) when the tag was applied. */
	val date: Long? = null,
	/** The identifier of the user who applied the tag. */
	val userId: String? = null,
	/** The display name of the user who applied the tag. */
	val userName: String? = null,
	/** The base64-encoded encrypted content of this tag. */
	override val encryptedSelf: Base64StringDto? = null,
) : Serializable,
	EncryptableDto
