package org.taktik.icure.entities.requests

import org.taktik.icure.entities.ExchangeData
import org.taktik.icure.entities.utils.Base64String
import org.taktik.icure.entities.utils.KeypairFingerprintString

/**
 * Request to create a piece of exchange data, for a certain recipient of a simple-type data owner group.
 */
data class ExchangeDataPieceCreationRequest(
	val exchangeKey: Map<KeypairFingerprintString, Base64String>,
	val accessControlSecret: Map<KeypairFingerprintString, Base64String>,
	val sharedSignatureKey: Map<KeypairFingerprintString, Base64String>,
	/**
	 * Must be empty except on the piece where the recipient is the delegator. Empty there as well to create exchange
	 * data that is already invalidated: see [ExchangeData.delegatorSignature].
	 */
	val delegatorSignature: Map<KeypairFingerprintString, Base64String>,
	val sharedSignature: Base64String,
)