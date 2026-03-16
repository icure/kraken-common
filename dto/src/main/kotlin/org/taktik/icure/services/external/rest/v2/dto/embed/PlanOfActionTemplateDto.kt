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
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEndOfLifeDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.NamedDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a template for a plan of action, defining a reusable structure of forms and metadata
 * that can be applied to healthcare elements.
 */
data class PlanOfActionTemplateDto(
	/** The unique identifier of this plan of action template. */
	override val id: String,
	/** The timestamp (unix epoch in ms) of creation. */
	override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification. */
	override val modified: Long? = null,
	/** The identifier of the author. */
	override val author: String? = null,
	/** The identifier of the responsible entity. */
	override val responsible: String? = null,
	/** Deprecated. The identifier of the medical location. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** The set of tags associated with this template. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** The set of codes associated with this template. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** The soft-delete timestamp. */
	override val endOfLife: Long? = null,
	// Usually one of the following is used (either valueDate or openingDate and closingDate)
	/** The name of this plan of action template. */
	override val name: String? = null,
	/** A description of the template. */
	val descr: String? = null,
	/** A note associated with the template. */
	val note: String? = null,
	/** Whether this template is relevant. */
	@param:Schema(defaultValue = "true") val relevant: Boolean = true,
	/** A bitmask status (bit 0: active/inactive, bit 1: relevant/irrelevant, bit 2: present/absent). */
	@param:Schema(defaultValue = "0") val status: Int = 0, // bit 0: active/inactive, bit 1: relevant/irrelevant, bit 2 : present/absent, ex: 0 = active,relevant and present
	/** The list of form skeletons that compose this template. */
	var forms: List<FormSkeletonDto> = emptyList(),
) : ICureDocumentDto<String>,
	NamedDto,
	HasEndOfLifeDto
