/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.gui.layout

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

/**
 * Created by aduchate on 07/02/13, 17:10
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class FormSection : Serializable {
	var icon: String? = null
	var title: String? = null
	var columns: Int? = null
	var formColumns: List<FormColumn> = ArrayList()
}
