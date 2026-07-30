map = function (doc) {

    var emit_for_delegates = function (doc, emitWithDelegateAndDoc) {
        const delegates = new Set()
        if (doc.securityMetadata) {
            const metadata = doc.securityMetadata
            if (metadata.secureDelegations) {
                for (const [delegationKey, secureDelegation] of Object.entries(metadata.secureDelegations)) {
                    if (secureDelegation.delegate) {
                        delegates.add(secureDelegation.delegate)
                    }
                    if (!secureDelegation.delegate || !secureDelegation.delegator) {
                        delegates.add(delegationKey)
                    }
                }
            }
        }
        if (doc.delegations) {
            Object.keys(doc.delegations).forEach(function (k) {
                delegates.add(k)
            });
        }
        for (const delegate of delegates) {
            emitWithDelegateAndDoc(delegate, doc)
        }
    }

    if (doc.java_type === 'org.taktik.icure.entities.RelatedPerson' && !doc.deleted) {
        emit_for_delegates(doc, function (dataOwnerId, doc) {
            emit([dataOwnerId], null)
        })
    }
};
