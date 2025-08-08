/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.gui.editor

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v1.dto.gui.Editor

/**
 * Created by aduchate on 03/12/13, 17:42
 */
@JsonPolymorphismRoot(Editor::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class SubFormEditor(
	val optionalFormGuids: List<String>? = null,
	val compulsoryFormGuids: List<String>? = null,
	val growsHorizontally: Boolean? = null,
	val collapsed: Boolean? = null,
	val showHeader: Boolean = true,
) : Editor()
