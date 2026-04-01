/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

import java.io.Serializable

class GetFormLayoutAndContentRequest : Serializable {
	var formTemplateGuid: String? = null
	var entityClass: String? = null
	var entityId: String? = null

	constructor() : super() {}
	constructor(formTemplateGuid: String?, entityClass: String?, entityId: String?) : super() {
		this.formTemplateGuid = formTemplateGuid
		this.entityClass = entityClass
		this.entityId = entityId
	}

	companion object {
		private const val serialVersionUID = 1L
	}
}
