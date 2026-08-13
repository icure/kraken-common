map = function(doc) {
	if (doc.java_type === 'org.taktik.icure.entities.ExchangeData' && !doc.deleted && (doc.delegator && doc.delegate)) {
		var participants = doc.delegate !== doc.delegator ? [doc.delegator, doc.delegate] : [doc.delegator]
		// Only the pieces of exchange data for a simple-type group have a recipient; any other exchange data, including
		// the exchange data for parent-type groups, emits null so it is never matched by a recipient filter.
		for (var i = 0; i < participants.length; i++) {
			emit([participants[i], doc.recipient || null], null)
		}
	}
}
