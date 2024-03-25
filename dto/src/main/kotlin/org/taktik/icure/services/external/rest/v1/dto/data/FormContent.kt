/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.CodeDto

/**
 * Created by aduchate on 01/02/13, 12:20
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FormContent(
	override val id: String? = null,
	override val entityClass: String? = null,
	override val entityId: String? = null,
	override val label: String? = null,
	override val index: Int? = null,
	override val guid: String? = null,
	override val tags: List<CodeDto>? = null,
	val formTemplateGuid: String? = null,
	val dashboardGuid: String? = null,
	val dataJXPath: String? = null,
	val descr: String? = null,
	val isAllowMultiple: Boolean = false,
	val isDeleted: Boolean = false,
	val items: List<IFormItem> = emptyList()
) : FormItem(label, index, guid, tags), DisplayableContent
