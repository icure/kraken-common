/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FlatRateTarificationDto(
	val code: String? = null,
	val flatRateType: FlatRateTypeDto? = null,
	val label: Map<String, String>? = null,
	val valorisations: List<ValorisationDto> = emptyList(),
	override val encryptedSelf: String? = null,
) : EncryptableDto,
	Serializable
