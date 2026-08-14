map = function(doc) {
	if (doc.java_type === 'org.taktik.icure.entities.ExchangeData' && !doc.deleted && (doc.delegator && doc.delegate)) {
		// Only the pieces of exchange data for a simple-type group have a recipient. Any other exchange data, including
		// the exchange data for parent-type groups, has no recipient field at all: it is normalised to null here, so
		// that it still produces exactly one row and is matched only by a filter on the null recipient.
		var recipient = doc.recipient == null ? null : doc.recipient
		emit([doc.delegator, doc.delegate, recipient], null)
	}
}
