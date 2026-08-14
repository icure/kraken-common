map = function(doc) {
	if (doc.java_type === 'org.taktik.icure.entities.ExchangeData' && !doc.deleted && (doc.delegator && doc.delegate)) {
		// Exchange data that is not for a simple-type group has no group id: it is its own single-piece group, keyed by
		// its own id, so that a lookup by id and a lookup by group id are the same query.
		var groupId = doc.exchangeDataGroupId == null ? doc._id : doc.exchangeDataGroupId
		// Such exchange data has no recipient field either: it is normalised to null here, so that it still produces
		// exactly one row and is matched only by a filter on the null recipient, while staying reachable unfiltered.
		var recipient = doc.recipient == null ? null : doc.recipient
		emit([groupId, recipient], null)
	}
}
