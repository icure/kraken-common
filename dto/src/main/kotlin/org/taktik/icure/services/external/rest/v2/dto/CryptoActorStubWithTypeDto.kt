package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@JsonIgnoreProperties(ignoreUnknown = true)
data class CryptoActorStubWithTypeDto(
	@get:Schema(required = true) val type: DataOwnerTypeDto,
	@get:Schema(required = true) val stub: CryptoActorStubDto,
)
