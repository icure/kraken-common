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
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a letter-based value used in tarification, associating a letter key with an index, coefficient, and numeric value.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.embed.LetterValueDto")
data class LetterValueDto(
	/** The letter key identifier. */
	@ActiveField val letter: String? = null,
	/** The index associated with this letter value. */
	@ActiveField val index: String? = null,
	/** The coefficient multiplier. */
	@ActiveField val coefficient: Double? = null,
	/** The numeric value. */
	@ActiveField val value: Double? = null,
)
