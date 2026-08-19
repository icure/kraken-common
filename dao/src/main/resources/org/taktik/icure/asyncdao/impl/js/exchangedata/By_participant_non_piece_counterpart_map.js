map = function(doc) {
	if (doc.java_type === 'org.taktik.icure.entities.ExchangeData' && !doc.deleted && (doc.delegator && doc.delegate)) {
		// Only the exchange data that a data owner creates and re-encrypts with its own keypairs is indexed. The
		// pieces of a simple-type group are the only exchange data with a recipient, and they are created and
		// re-encrypted through the group pieces flow instead, so the group is deliberately never reported as a
		// counterpart by this view.
		if (doc.recipient != null) return
		// A data owner is never its own counterpart.
		if (doc.delegator === doc.delegate) return
		// A keypair can only be used with this exchange data if it has an entry in all three secret maps; the reduce
		// intersects these across every exchange data of a counterpart, so a fingerprint survives only if all of them
		// are usable with it.
		var usableFingerprints = []
		var exchangeKey = doc.exchangeKey || {}
		var accessControlSecret = doc.accessControlSecret || {}
		var sharedSignatureKey = doc.sharedSignatureKey || {}
		/*
		 * Couchdb rejects a reduce whose output is over 4096 bytes (5000 and configurable from 3.5) and does not at
		 * least halve its input, and the intersection of a lone exchange data is its own list, so a list past ~116
		 * fingerprints of 32 hex characters would turn this counterpart's row into a reduce_overflow_error instead of
		 * a value.
		 *
		 * The usable fingerprints are by construction a subset of the keys of exchangeKey, so measuring those is enough
		 * to know the list would be over the cap, and measuring them first is what lets an oversized exchange data be
		 * skipped without walking it at all. Over the cap nothing is collected and the list stays empty, which the
		 * reduce propagates on its own: no keypair is then ever excluded for this counterpart, so it is always
		 * reported. That is the safe direction, and the only thing lost is the keypair filter for that one counterpart.
		 */
		if (Object.keys(exchangeKey).length <= 100) {
			for (var fingerprint in exchangeKey) {
				if (accessControlSecret.hasOwnProperty(fingerprint) && sharedSignatureKey.hasOwnProperty(fingerprint)) {
					usableFingerprints.push(fingerprint)
				}
			}
		}
		var pairs = [[doc.delegator, doc.delegate], [doc.delegate, doc.delegator]]
		for (var i = 0; i < pairs.length; i++) {
			var participant = pairs[i][0]
			var counterpart = pairs[i][1]
			// The counterpart is split into the group it belongs to and its id in that group, so that the counterparts
			// which are in the same group as the participant are a contiguous range: null sorts before every group id.
			// The participant is left whole instead: a query always has its full reference and never ranges over it.
			var separator = counterpart.indexOf('/')
			if (separator < 0) {
				emit([participant, null, counterpart], usableFingerprints)
			} else {
				emit([participant, counterpart.substring(0, separator), counterpart.substring(separator + 1)], usableFingerprints)
			}
		}
	}
}
