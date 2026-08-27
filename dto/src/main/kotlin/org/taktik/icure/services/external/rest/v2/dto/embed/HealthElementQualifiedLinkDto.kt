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
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * A directed, qualified link from one healthcare element to another. Links should be created in a single direction:
 * the reverse link can be found through a view.
 */
data class HealthElementQualifiedLinkDto(
	/** The qualification of the link. Free string; using the names of LinkQualification entries is encouraged but not enforced. */
	@ActiveField val type: String,
	/** A caller-chosen correlation id that groups related links across entities. */
	@ActiveField val associationId: String? = null,
	/** The id of the linked healthcare element. */
	@ActiveField val healthElementId: String,
) : Serializable
