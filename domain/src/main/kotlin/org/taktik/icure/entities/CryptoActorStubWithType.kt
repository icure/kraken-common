package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CryptoActorStubWithType(
	/** The type of data owner (healthcare party, device, or patient). */
	val type: DataOwnerType,
	/** The cryptographic actor stub containing encryption keys and related metadata. */
	val stub: CryptoActorStub,
)
