package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CryptoActorStubWithTypeDto(
    val type: DataOwnerTypeDto,
    val stub: CryptoActorStubDto
)
