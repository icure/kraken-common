map = function(doc) {
    if (doc.java_type === 'org.taktik.icure.entities.ExchangeData' && !doc.deleted && (doc.delegator && doc.delegate)) {
        var participants = doc.delegate !== doc.delegator ? [doc.delegator, doc.delegate] : [doc.delegator]
        if (doc.recipient == null || doc.recipient == doc.delegator) {
            for (var i = 0; i < participants.length; i++) {
                emit(participants[i], null)
            }
        }
    }
}
