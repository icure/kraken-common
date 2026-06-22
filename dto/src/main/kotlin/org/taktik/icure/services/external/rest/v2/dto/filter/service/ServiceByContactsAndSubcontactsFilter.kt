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
package org.taktik.icure.services.external.rest.v2.dto.filter.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.embed.ServiceDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Deprecated("This filter is deprecated")
/**
 * Deprecated filter that matches services by healthcare party, contacts, sub-contacts, and value date range.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByContactsAndSubcontactsFilter")
data class ServiceByContactsAndSubcontactsFilter(
	/** Optional description of this filter. */
	override val desc: String? = null,
	/** The identifier of the healthcare party. */
	@ActiveField val healthcarePartyId: String? = null,
	/** The set of contact identifiers to match. */
	@param:Schema(required = true)
	@ActiveField val contacts: Set<String>,
	/** The set of sub-contact identifiers to match. */
	@ActiveField val subContacts: Set<String>? = null,
	/** The start of the value date range. */
	@ActiveField val startValueDate: Long? = null,
	/** The end of the value date range. */
	@ActiveField val endValueDate: Long? = null,
) : AbstractFilterDto<ServiceDto>
