/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.gui.layout

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.gui.Tag
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class FormLayout : Serializable {
	var name: String? = null
	var width: Double? = null
	var height: Double? = null
	var descr: String? = null
	var tag: Tag? = null
	var guid: String? = null
	var group: String? = null
	var sections: List<FormSection> = ArrayList()
	var importedServiceXPaths: List<String>? = null
}
