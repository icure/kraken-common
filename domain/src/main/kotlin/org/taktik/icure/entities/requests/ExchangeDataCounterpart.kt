package org.taktik.icure.entities.requests

import org.taktik.icure.entities.ExchangeData
import org.taktik.icure.entities.utils.KeypairFingerprintString

/**
 * A data owner that another data owner shares [ExchangeData] with, and how usable that exchange data is.
 *
 * This is an internal result type: the counterparts searches only ever return [counterpartId] to their callers,
 * [usableKeypairFingerprints] exists so that the logic can apply the keypair filter without loading any document.
 */
data class ExchangeDataCounterpart(
	/**
	 * Id of the counterpart. This is the [ExchangeData.delegate] of the exchange data where the data owner the search
	 * was made for is the [ExchangeData.delegator], and vice versa.
	 */
	val counterpartId: String,
	/**
	 * Fingerprints of the keypairs that **every** exchange data between the data owner the search was made for and
	 * [counterpartId] is fully usable with, that is the keypairs having an entry in all of [ExchangeData.exchangeKey],
	 * [ExchangeData.accessControlSecret] and [ExchangeData.sharedSignatureKey] of each of them.
	 *
	 * A keypair missing from this set has at least one exchange data with this counterpart that can't be fully used
	 * with it.
	 */
	val usableKeypairFingerprints: Set<KeypairFingerprintString>,
)
