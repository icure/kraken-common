package org.taktik.icure.services.external.rest.v1.dto.gui.layout

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class FormColumn(
	val formDataList: MutableList<FormLayoutData> = ArrayList(),
) : Serializable {
	/**
	 * Determines the columns span of the object
	 *
	 * @param columns: 1=column 1, 1-2=column 1 and 2. Null means all columns.
	 */
	var columns: String? = null
	var shouldDisplay: Boolean? = null

	fun addFormData(fd: FormLayoutData) {
		formDataList.add(fd)
	}
}
