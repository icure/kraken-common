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
import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a suspension period for a medication, including start and end moments, reason, and lifecycle state.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.embed.SuspensionDto")
data class SuspensionDto(
	/** The start moment of the suspension (fuzzy date). */
	@ActiveField val beginMoment: Long? = null,
	/** The end moment of the suspension (fuzzy date). */
	@ActiveField val endMoment: Long? = null,
	/** The reason for the suspension. */
	@ActiveField val suspensionReason: String? = null,
	/** The lifecycle state of the suspension. */
	@ActiveField val lifecycle: String? = null,
) : Serializable
