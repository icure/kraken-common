package org.taktik.icure.services.external.rest.v2.dto.requests

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.KeypairFingerprintV2StringDto

/**
 * Request to create a piece of exchange data, for a certain recipient of a simple-type data owner group.
 * Unlike [ExchangeDataPieceCreationRequestDto], which is a piece of a group named by the request that carries it, this
 * request stands on its own: it names the group the piece belongs to and all of its participants, so that a single
 * bulk request can create pieces for many exchange data groups at once.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = false)
data class BulkExchangeDataPieceCreationRequestDto(
	@param:Schema(required = true)
	@ActiveField val exchangeKey: Map<KeypairFingerprintV2StringDto, Base64StringDto>,
	@param:Schema(required = true)
	@ActiveField val accessControlSecret: Map<KeypairFingerprintV2StringDto, Base64StringDto>,
	@param:Schema(required = true)
	@ActiveField val sharedSignatureKey: Map<KeypairFingerprintV2StringDto, Base64StringDto>,
	/**
	 * Id of the exchange data group this piece belongs to. The piece where the recipient is the delegator anchors the
	 * group and has this as its own id; every other piece of the group has a derived id.
	 */
	@param:Schema(required = true)
	@ActiveField val exchangeDataGroupId: String,
	@param:Schema(required = true)
	@ActiveField val delegator: String,
	@param:Schema(required = true)
	@ActiveField val delegate: String,
	/**
	 * The member of the delegate group this piece is for. All the pieces of a group must agree on the delegator and
	 * the delegate, and no two pieces of the same request may share a group and a recipient.
	 */
	@param:Schema(required = true)
	@ActiveField val recipient: String,
	/**
	 * Must be empty except on the piece of exchange data where the recipient is the delegator. Empty there as well to
	 * create exchange data that is already permanently invalidated: it will never be used to encrypt new data.
	 */
	@ActiveField val delegatorSignature: Map<KeypairFingerprintV2StringDto, Base64StringDto> = emptyMap(),
	/**
	 * Must be null except on the piece of exchange data where the recipient is the delegator: this signature is only
	 * used to decide if the exchange data can be trusted for encryption, and that decision is taken on the delegator
	 * piece alone.
	 */
	@ActiveField val sharedSignature: Base64StringDto? = null,
)
