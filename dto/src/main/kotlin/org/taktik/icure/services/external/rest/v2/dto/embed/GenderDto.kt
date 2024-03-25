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

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

enum class GenderDto(val code: String) : Serializable {
	@Schema(defaultValue = "M") male("M"),
	@Schema(defaultValue = "F") female("F"),
	@Schema(defaultValue = "I") indeterminate("I"),
	@Schema(defaultValue = "C") changed("C"),
	@Schema(defaultValue = "Y") changedToMale("Y"),
	@Schema(defaultValue = "X") changedToFemale("X"),
	@Schema(defaultValue = "U") unknown("U");

	companion object {
		fun fromCode(code: String?): GenderDto? {
			return code?.let { c -> entries.find { c == it.code } }
		}
	}
}
