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
package org.taktik.icure.services.external.rest.v2.dto.filter.contact

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.ContactDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches contacts by healthcare party, patient secret foreign keys, tag, code, and opening date range.
 */
data class ContactByHcPartyPatientTagCodeDateFilter(
	/** Optional description of this filter. */
	override val desc: String? = null,
	/** The identifier of the healthcare party. */
	val healthcarePartyId: String? = null,
	/** Deprecated. Use patientSecretForeignKeys instead. */
	@Deprecated("Use patientSecretForeignKeys instead")
	val patientSecretForeignKey: String? = null,
	/** The list of secret foreign keys for patient matching. */
	val patientSecretForeignKeys: List<String>? = null,
	/** The type of the tag to filter on. */
	val tagType: String? = null,
	/** The tag code value to match. */
	val tagCode: String? = null,
	/** The type of the code to filter on. */
	val codeType: String? = null,
	/** The code value to match. */
	val codeCode: String? = null,
	/** The start of the contact opening date range. */
	val startOfContactOpeningDate: Long? = null,
	/** The end of the contact opening date range. */
	val endOfContactOpeningDate: Long? = null,
) : AbstractFilterDto<ContactDto>
