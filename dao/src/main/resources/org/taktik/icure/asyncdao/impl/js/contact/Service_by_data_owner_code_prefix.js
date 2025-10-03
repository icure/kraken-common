function(doc) {
  if (doc.java_type === 'org.taktik.icure.entities.Contact' && !doc.deleted) {

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

    doc.services.forEach(function (service) {
      if (service.codes && service.codes.length && service._id != null) {
        service.codes.forEach(function (code) {
          for (const delegate of delegates) {
            emit([delegate, code.type, code.code], service._id);
          }
        });
      }
    });
  }
}
