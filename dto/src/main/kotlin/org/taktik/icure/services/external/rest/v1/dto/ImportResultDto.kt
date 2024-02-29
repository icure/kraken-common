/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.base.MimeAttachmentDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ImportResultDto(
	val patient: PatientDto? = null,
	val hes: List<HealthElementDto> = emptyList(),
	val ctcs: List<ContactDto> = emptyList(),
	val warnings: List<String> = emptyList(),
	val errors: List<String> = emptyList(),
	val forms: List<FormDto> = emptyList(),
	val hcps: List<HealthcarePartyDto> = emptyList(),
	val documents: List<DocumentDto> = emptyList(),
	val attachments: Map<String, MimeAttachmentDto> = emptyMap()
)
