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
 * Represents a flat rate tarification used in medical house contracts, linking a code to its valorisations.
 */
data class FlatRateTarificationDto(
	/** The tarification code. */
	val code: String? = null,
	/** The type of flat rate (physician, physiotherapist, nurse, or ptd). */
	val flatRateType: FlatRateTypeDto? = null,
	/** Localized labels for this tarification, keyed by language code. */
	val label: Map<String, String>? = null,
	/** The list of valorisations associated with this tarification. */
	val valorisations: List<ValorisationDto> = emptyList(),
	/** The base64-encoded encrypted content. */
	override val encryptedSelf: Base64StringDto? = null,
) : EncryptableDto,
	Serializable
