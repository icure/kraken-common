function(doc) {

    if (doc.java_type === 'org.taktik.icure.entities.Message' && !doc.deleted) {
        let emittedTagTypes
        emittedTagTypes = new Set()

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
        doc.tags.forEach(function (tag) {
            for (const delegate of delegates) {
                if (!emittedTagTypes.has(tag.type)) {
                    emit([delegate, tag.type], null);
                    emittedTagTypes.add(tag.type)
                }
                emit([delegate, tag.type, tag.code], null);                }
        })
    }
}