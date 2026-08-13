package org.taktik.icure.services.external.rest.v2.dto.requests

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.KeypairFingerprintV2StringDto

/**
 * Request to create a piece of exchange data, for a certain recipient of a simple-type data owner group.
 * The id, recipient, delegator, delegate and exchange data group id of the created piece are not part of this request:
 * they come from the parameters of the request to create the pieces of an exchange data group, and from the key this
 * request is associated to.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = false)
data class ExchangeDataPieceCreationRequestDto(
	@param:Schema(required = true)
	@ActiveField val exchangeKey: Map<KeypairFingerprintV2StringDto, Base64StringDto>,
	@param:Schema(required = true)
	@ActiveField val accessControlSecret: Map<KeypairFingerprintV2StringDto, Base64StringDto>,
	@param:Schema(required = true)
	@ActiveField val sharedSignatureKey: Map<KeypairFingerprintV2StringDto, Base64StringDto>,
	/**
	 * Must be empty except on the piece of exchange data where the recipient is the delegator.
	 */
	@ActiveField val delegatorSignature: Map<KeypairFingerprintV2StringDto, Base64StringDto> = emptyMap(),
	@param:Schema(required = true)
	@ActiveField val sharedSignature: Base64StringDto,
	@ActiveField val invalidated: Boolean = false,
)
