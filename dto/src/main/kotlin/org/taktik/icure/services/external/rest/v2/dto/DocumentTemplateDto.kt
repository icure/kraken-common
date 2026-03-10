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
import org.taktik.icure.services.external.rest.v2.dto.base.ReportVersionDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DocumentGroupDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DocumentTypeDto
import org.taktik.icure.utils.DynamicInitializer

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a template for generating documents. Document templates define the structure, type, and content
 * that can be used to produce actual documents.
 */
data class DocumentTemplateDto(
	/** The Id of the document template. */
	override val id: String,
	/** The revision of the document template in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation of this entity. */
	override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification of this entity. */
	override val modified: Long? = null,
	/** The id of the User that created this document template. */
	override val author: String? = null,
	/** The id of the data owner that is responsible for this document template. */
	override val responsible: String? = null,
	/** The medical location where this entity was created. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** Tags that qualify the document template as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this particular document template. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	override val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** The binary content of the template attachment. */
	@param:Schema(type = "string", format = "byte") val attachment: ByteArray? = null,
	/** The type of document (e.g., admission, clinical path, document report, invoice). */
	@param:Schema(description = "The type of document, ex: admission, clinical path, document report,invoice, etc.") val documentType: DocumentTypeDto? = null,
	/** The main Uniform Type Identifier of the template attachment. */
	val mainUti: String? = null,
	/** The name of the document template. */
	val name: String? = null,
	/** Extra Uniform Type Identifiers for the template attachment. */
	val otherUtis: Set<String> = emptySet(),
	/** The id of the CouchDB attachment for the template. */
	val attachmentId: String? = null,
	/** The version of the report template. */
	val version: ReportVersionDto? = null,
	/** The owner of the document template. */
	val owner: String? = null,
	/** A globally unique identifier for the template. */
	val guid: String? = null,
	/** The document group this template belongs to. */
	val group: DocumentGroupDto? = null,
	/** A description of the document template. */
	val descr: String? = null,
	/** Whether this template is disabled. */
	val disabled: String? = null,
	/** The medical specialty associated with this template. */
	val specialty: CodeStubDto? = null,
) : StoredDocumentDto,
	ICureDocumentDto<String>,
	HasEndOfLifeDto {
	companion object : DynamicInitializer<DocumentTemplateDto>

	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
