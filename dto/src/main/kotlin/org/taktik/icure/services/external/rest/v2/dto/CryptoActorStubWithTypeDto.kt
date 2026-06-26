package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * DTO that pairs a crypto actor stub with its data owner type, used for identifying data owners
 * and their cryptographic metadata.
 */
data class CryptoActorStubWithTypeDto(
	/** The type of data owner (healthcare party, device, or patient). */
	@param:Schema(required = true) @ActiveField val type: DataOwnerTypeDto,
	/** The cryptographic actor stub containing encryption keys and related metadata. */
	@param:Schema(required = true) @ActiveField val stub: CryptoActorStubDto,
)
