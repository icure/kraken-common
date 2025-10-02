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
      if (service.tags && service.tags.length && service._id != null) {
        service.tags.forEach(function (tag) {
          for (const delegate of delegates) {
            emit([delegate, tag.type, tag.code], [service._id, d]);
          }
        });
      }
    });
  }
}
