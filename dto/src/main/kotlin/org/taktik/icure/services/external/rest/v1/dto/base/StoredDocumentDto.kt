/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.base

import io.swagger.v3.oas.annotations.media.Schema

interface StoredDocumentDto : VersionableDto<String> {
	@get:Schema(description = "hard delete (unix epoch in ms) timestamp of the object.")
	val deletionDate: Long?

	fun withDeletionDate(deletionDate: Long?): StoredDocumentDto
}
