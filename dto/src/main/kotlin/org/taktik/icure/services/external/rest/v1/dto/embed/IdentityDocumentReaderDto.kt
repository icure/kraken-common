/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.embed

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class IdentityDocumentReaderDto(
	val justificatifDocumentNumber: String? = null,
	val supportSerialNumber: String? = null,
	val timeReadingEIdDocument: Long? = null,
	@Schema(defaultValue = "0") val eidDocumentSupportType: Int = 0,
	@Schema(defaultValue = "0") val reasonManualEncoding: Int = 0,
	@Schema(defaultValue = "0") val reasonUsingVignette: Int = 0
) : Serializable
