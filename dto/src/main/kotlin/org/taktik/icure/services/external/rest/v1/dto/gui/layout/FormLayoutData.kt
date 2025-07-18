/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.gui.layout

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.embed.ContentDto
import org.taktik.icure.services.external.rest.v1.dto.gui.Editor
import org.taktik.icure.services.external.rest.v1.dto.gui.FormDataOption
import org.taktik.icure.services.external.rest.v1.dto.gui.FormPlanning
import org.taktik.icure.services.external.rest.v1.dto.gui.Formula
import org.taktik.icure.services.external.rest.v1.dto.gui.GuiCode
import org.taktik.icure.services.external.rest.v1.dto.gui.GuiCodeType
import org.taktik.icure.services.external.rest.v1.dto.gui.Suggest
import java.io.Serializable

/**
 * Created by aduchate on 19/11/13, 10:50
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
open class FormLayoutData(
	val isSubForm: Boolean? = null,
	val isIrrelevant: Boolean? = null,
	val isDeterminesSscontactName: Boolean? = null,
	val type: String? = null,
	val name: String? = null,
	val sortOrder: Double? = null,
	val options: Map<String, FormDataOption>? = null,
	val descr: String? = null,
	val label: String? = null,
	val editor: Editor? = null,
	val defaultValue: List<ContentDto>? = null,
	val defaultStatus: Int? = null,
	val suggest: List<Suggest>? = null,
	val plannings: List<FormPlanning>? = null,
	val tags: List<GuiCode>? = null,
	val codes: List<GuiCode>? = null,
	val codeTypes: List<GuiCodeType>? = null,
	val formulas: List<Formula>? = null,
) : Serializable
