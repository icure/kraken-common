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
                const responsibleId = Object.entries(metadata.secureDelegations).reduce((acc, [, sd]) => acc ? acc : sd.delegator === sd.delegate ? sd.delegator : null , null)

                for (const [delegationKey, secureDelegation] of Object.entries(metadata.secureDelegations)) {
                    if (secureDelegation.delegate) {
                        if (!emittedDataOwners.has(secureDelegation.delegate)) {
                            emittedDataOwners.add(secureDelegation.delegate)
                            emitWithDelegateAndDoc(secureDelegation.delegate, responsibleId, doc)
                        }
                    }
                    if (secureDelegation.delegator) {
                        if (!emittedDataOwners.has(secureDelegation.delegator)) {
                            emittedDataOwners.add(secureDelegation.delegator)
                            emitWithDelegateAndDoc(secureDelegation.delegator, responsibleId, doc)
                        }
                    }
                    if (!secureDelegation.delegate || !secureDelegation.delegator) {
                        emitWithDelegateAndDoc(delegationKey, responsibleId, doc)
                        const equivalences = equivalencesByCanonical[delegationKey]
                        if (equivalences) {
                            equivalences.forEach(function (equivalence) { emitWithDelegateAndDoc(equivalence, responsibleId, doc) })
                        }
                    }
                }
            }
        }
        if (doc.delegations) {
            const responsibleId = Object.entries(doc.delegations).reduce((acc, [, sd]) => acc ? acc : sd.reduce((acc, sd) => sd.owner === sd.delegatedTo ? sd.owner : null, null )  , null)

            Object.keys(doc.delegations).forEach(function (k) {
                if(!emittedDataOwners.has(k)) {
                    // No reason to do add: keys from doc.delegations can't be duplicated, and doc.delegations are the last thing we emit
                    emitWithDelegateAndDoc(k, responsibleId, doc)
                }
            });
        }
    }

    if (doc.java_type === 'org.taktik.icure.entities.CalendarItem' && (doc.deleted != null || doc.created != null || doc.modified != null)) {
        const latestUpdate = [doc.deleted, doc.created, doc.modified].reduce((p, c) => (c ? c : 0) > p ? c : p, 0)
        emit_for_delegates(doc, function (dataOwnerId, responsibleId) {
            emit([dataOwnerId, latestUpdate], responsibleId)
        })
    }
}
