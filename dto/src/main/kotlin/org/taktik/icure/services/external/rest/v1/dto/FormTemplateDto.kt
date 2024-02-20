/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

/**
 * Created by aduchate on 09/07/13, 16:27
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v1.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DocumentGroupDto
import org.taktik.icure.services.external.rest.v1.dto.gui.layout.FormLayout

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FormTemplateDto(
	override val id: String,
	override val rev: String? = null,
	override val deletionDate: Long? = null,

	@Deprecated("Use templateLayout")
	val layout: FormLayout? = null,

	val templateLayout: org.taktik.icure.services.external.rest.v1.dto.embed.form.template.FormTemplateLayout? = null,
	val rawTemplateLayout: ByteArray? = null, //Base64 representation of a form template layout

	val name: String? = null,
	//Globally unique and consistent across all DBs that get their formTemplate from a icure cloud library
	//The id is not guaranteed to be consistent across dbs
	val guid: String? = null,
	val group: DocumentGroupDto? = null,
	val descr: String? = null,
	val disabled: String? = null,
	val specialty: CodeStubDto? = null,
	val author: String? = null,
	//Location in the form of a gpath/xpath like location with an optional action
	//ex: healthElements[codes[type == 'ICD' and code == 'I80']].plansOfAction[descr='Follow-up'] : add inside the follow-up plan of action of a specific healthElement
	//ex: healthElements[codes[type == 'ICD' and code == 'I80']].plansOfAction += [descr:'Follow-up'] : create a new planOfAction and add inside it
	val formInstancePreferredLocation: String? = null,
	val keyboardShortcut: String? = null,
	val shortReport: String? = null,
	val mediumReport: String? = null,
	val longReport: String? = null,
	val reports: Set<String> = emptySet(),
	val tags: Set<CodeStubDto> = emptySet(),
	val layoutAttachmentId: String? = null,
	val templateLayoutAttachmentId: String? = null
) : StoredDocumentDto {
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
