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
        doc.codes.forEach(function (code) {
            for (const delegate of delegates) {
                if (!emittedTagTypes.has(code.type)) {
                    emit([delegate, code.type], null);
                    emittedTagTypes.add(code.type)
                }
                emit([delegate, code.type, code.code], null);                }
        })
    }
}
