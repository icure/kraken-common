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
package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEndOfLifeDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.PlanOfActionTemplateDto

@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a template for creating healthcare elements. Health element templates provide a predefined structure
 * for commonly used healthcare elements, including plans of action and default status values.
 */
data class HealthElementTemplateDto(
	/** The unique identifier of the health element template. */
	override val id: String,
	/** The revision of the template in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation. */
	override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification. */
	override val modified: Long? = null,
	/** The id of the User that created this template. */
	override val author: String? = null,
	/** The id of the HealthcareParty that is responsible for this template. */
	override val responsible: String? = null,
	/** The id of the medical location where this template was created. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** Tags that qualify the template as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this particular template. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	override val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** Description of the health element template. */
	val descr: String? = null,
	/** A text note associated with the template. */
	val note: String? = null,
	/** Bit field: bit 0 = active/inactive, bit 1 = relevant/irrelevant, bit 2 = present/absent. */
	@param:Schema(defaultValue = "0") val status: Int = 0, // bit 0: active/inactive, bit 1: relevant/irrelevant, bit 2 : present/absent, ex: 0 = active,relevant and present
	/** Whether the health element template is relevant. */
	@param:Schema(defaultValue = "true") val relevant: Boolean = true,
	/** List of plan of action templates associated with this health element template. */
	val plansOfAction: List<PlanOfActionTemplateDto> = emptyList(),
) : StoredDocumentDto,
	ICureDocumentDto<String>,
	HasEndOfLifeDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
