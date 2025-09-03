function(doc) {

  var emit_for_delegates = function (doc, emitWithDelegateAndDoc) {
    let emittedDataOwners
    emittedDataOwners = new Set()
    if (doc.securityMetadata) {
      const metadata = doc.securityMetadata
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
          }
        }
      }
    }
    if (doc.delegations) {
      Object.keys(doc.delegations).forEach(function (k) {
        if (!emittedDataOwners.has(k)) {
          emittedDataOwners.add(k)
          emitWithDelegateAndDoc(k, doc)
        }
      });
    }
  }

  var emit_services_by_sfk_code = function(hcparty, doc) {
    doc.secretForeignKeys.forEach(function (fk) {
      doc.services.forEach(function (service) {
        const d = service.valueDate ? service.valueDate : service.openingDate;
        if (service.codes && service.codes.length && service._id != null) {
          service.codes.forEach(function (code) {
            emit([hcparty, fk, code.type, code.code], [service._id, d]);
          });
        }
      });
    });
  };

  if (doc.java_type === 'org.taktik.icure.entities.Contact' && !doc.deleted) {
    emit_for_delegates(doc, function (dataOwnerId, doc) {
      emit_services_by_sfk_code(dataOwnerId, doc);
    })
  }
}
