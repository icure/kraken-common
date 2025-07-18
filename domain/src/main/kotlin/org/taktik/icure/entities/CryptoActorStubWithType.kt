package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CryptoActorStubWithType(
	val type: DataOwnerType,
	val stub: CryptoActorStub,
)
