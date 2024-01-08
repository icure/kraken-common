map = function (doc) {
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
    }

	var emit_patients_by_telecom = function (hcparty, doc) {
		doc.addresses.forEach(function (address) {
			if (address.telecoms && address.telecoms.length) {
				address.telecoms.forEach(function (telecom) {
					emit([hcparty, telecom.telecomNumber.replace(new RegExp('\\s', 'g'), '').replace(new RegExp('\\W', 'g'), '')], doc._id);
				});
			}
		});
	};

	if (doc.java_type === 'org.taktik.icure.entities.Patient' && !doc.deleted) {
		emit_for_delegates(doc, function (dataOwnerId, doc) {
			emit_patients_by_telecom(dataOwnerId, doc);
		})
	}
};
