package org.taktik.icure.services.external.rest.v2.dto.requests

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
data class EntityShareOrMetadataUpdateRequestDto(
	val share: EntityShareRequestDto? = null,
	val update: EntitySharedMetadataUpdateRequestDto? = null,
)
