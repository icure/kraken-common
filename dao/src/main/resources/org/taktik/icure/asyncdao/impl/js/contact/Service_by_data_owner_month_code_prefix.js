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
      let d = service.valueDate ? service.valueDate : service.openingDate;
      let year = d > 10000000000000 && d < 99991231235959 ? Math.floor(d / 10000000000) : null
      let month = d > 10000000000000 && d < 99991231235959 ? Math.floor(d / 100000000) % 100 : null
      if (month > 12) {
        month = null
        year = null
      }
      if (service.codes && service.codes.length && service._id != null) {
        service.codes.forEach(function (code) {
          for (const delegate of delegates) {
            emit([year, month, delegate, code.type, code.code], [service._id, d]);
          }
        });
      }
    });
  }
}
