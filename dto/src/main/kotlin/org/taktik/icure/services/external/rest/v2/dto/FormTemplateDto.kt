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
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DocumentGroupDto
import org.taktik.icure.services.external.rest.v2.dto.embed.form.template.FormTemplateLayout
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.FormTemplateDto")
data class FormTemplateDto(
	override val id: String,
	override val rev: String? = null,
	override val deletionDate: Long? = null,
	@ActiveField val templateLayout: FormTemplateLayout? = null,
	@ActiveField val rawTemplateLayout: ByteArray? = null, // Base64 representation of a form template layout
	@ActiveField val name: String? = null,
	// Globally unique and consistent accross all DBs that get their formTemplate from a icure cloud library
	// The id is not guaranteed to be consistent accross dbs
	@ActiveField val guid: String? = null,
	@ActiveField val group: DocumentGroupDto? = null,
	@ActiveField val descr: String? = null,
	@ActiveField val disabled: String? = null,
	@ActiveField val specialty: CodeStubDto? = null,
	@ActiveField val author: String? = null,
	// Location in the form of a gpath/xpath like location with an optional action
	// ex: healthElements[codes[type == 'ICD' and code == 'I80']].plansOfAction[descr='Follow-up'] : add inside the follow-up plan of action of a specific healthElement
	// ex: healthElements[codes[type == 'ICD' and code == 'I80']].plansOfAction += [descr:'Follow-up'] : create a new planOfAction and add inside it
	@ActiveField val formInstancePreferredLocation: String? = null,
	@ActiveField val keyboardShortcut: String? = null,
	@ActiveField val shortReport: String? = null,
	@ActiveField val mediumReport: String? = null,
	@ActiveField val longReport: String? = null,
	@ActiveField val reports: Set<String> = emptySet(),
	@ActiveField val tags: Set<CodeStubDto> = emptySet(),
	@ActiveField val layoutAttachmentId: String? = null,
	@ActiveField val templateLayoutAttachmentId: String? = null,
) : StoredDocumentDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
