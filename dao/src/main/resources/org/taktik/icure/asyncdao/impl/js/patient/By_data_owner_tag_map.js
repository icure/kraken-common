function(doc) {
    var emit_for_delegates = function (doc, emitWithDelegateAndDoc) {
        let emittedDataOwners
        emittedDataOwners = new Set()
        if (doc.securityMetadata) {
            const metadata = doc.securityMetadata
            let equivalencesByCanonical = {}
            if (metadata.keysEquivalences) {
                for (const [equivalentKey, canonicalKey] of Object.entries(metadata.keysEquivalences)) {
                    const prev = equivalencesByCanonical[canonicalKey]
                    if (prev) {
                        prev.push(equivalentKey)
                    } else {
                        equivalencesByCanonical[canonicalKey] = [equivalentKey]
                    }
                }
            }
            if (metadata.secureDelegations) {
                for (const [delegationKey, secureDelegation] of Object.entries(metadata.secureDelegations)) {
                    if (secureDelegation.delegate) {
                        if (!emittedDataOwners.has(secureDelegation.delegate)) {
                            emittedDataOwners.add(secureDelegation.delegate)
                            emitWithDelegateAndDoc(secureDelegation.delegate, doc)
                        }
                    }
                    if (secureDelegation.delegator) {
                        if (!emittedDataOwners.has(secureDelegation.delegator)) {
                            emittedDataOwners.add(secureDelegation.delegator)
                            emitWithDelegateAndDoc(secureDelegation.delegator, doc)
                        }
                    }
                    if (!secureDelegation.delegate || !secureDelegation.delegator) {
                        emitWithDelegateAndDoc(delegationKey, doc)
                        const equivalences = equivalencesByCanonical[delegationKey]
                        if (equivalences) {
                            equivalences.forEach(function (equivalence) { emitWithDelegateAndDoc(equivalence, doc) })
                        }
                    }
                }
            }
        }
        if (doc.delegations) {
            Object.keys(doc.delegations).forEach(function (k) {
                //No need to add because delegation is a map and keys cannot come twice
                //And this is the last possible source of data owners
                if (!emittedDataOwners.has(k)) {
                    emitWithDelegateAndDoc(k, doc)
                }
            });
        }
    }

    if (doc.java_type === 'org.taktik.icure.entities.Patient' && !doc.deleted) {
        emit_for_delegates(doc, function (dataOwnerId, doc) {
            let emittedTagTypes
            emittedTagTypes = new Set()

            doc.tags.forEach(function (tag) {
                if (!emittedTagTypes.has(tag.type)) {
                    emit([dataOwnerId, tag.type], null);
                    emittedTagTypes.add(tag.type)
                }
                emit([dataOwnerId, tag.type, tag.code], null);
            })
        })
    }
}
