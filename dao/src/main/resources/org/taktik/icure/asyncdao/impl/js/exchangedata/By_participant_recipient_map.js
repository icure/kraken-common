map = function(doc) {
	if (doc.java_type === 'org.taktik.icure.entities.ExchangeData' && !doc.deleted && (doc.delegator && doc.delegate)) {
		var participants = doc.delegate !== doc.delegator ? [doc.delegator, doc.delegate] : [doc.delegator]
		// Only the pieces of exchange data for a simple-type group have a recipient. Any other exchange data, including
		// the exchange data for parent-type groups, has no recipient field at all: it is normalised to null here, so
		// that it still produces exactly one row per participant and is matched only by a filter on the null recipient.
		var recipient = doc.recipient == null ? null : doc.recipient
		for (var i = 0; i < participants.length; i++) {
			emit([participants[i], recipient], doc.recipient == null || doc.recipient == doc.delegator ? null : doc.exchangeDataGroupId)
		}
	}
}
