package org.taktik.icure.entities.requests

import org.taktik.icure.entities.utils.Base64String
import org.taktik.icure.entities.utils.KeypairFingerprintString

/**
 * Request to create a piece of exchange data, for a certain recipient of a simple-type data owner group.
 */
data class ExchangeDataPieceCreationRequest(
	val exchangeKey: Map<KeypairFingerprintString, Base64String>,
	val accessControlSecret: Map<KeypairFingerprintString, Base64String>,
	val sharedSignatureKey: Map<KeypairFingerprintString, Base64String>,
	val delegatorSignature: Map<KeypairFingerprintString, Base64String>,
	val sharedSignature: Base64String,
	val invalidated: Boolean,
)