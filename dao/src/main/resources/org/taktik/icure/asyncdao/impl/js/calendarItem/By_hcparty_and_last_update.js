function(doc) {
    if (doc.java_type === 'org.taktik.icure.entities.CalendarItem' && (doc.deleted != null || doc.created != null || doc.modified != null)) {
        if (doc.delegations) {
            const latestUpdate = [doc.deleted, doc.created, doc.modified].reduce((p, c) => (c ? c : 0) > p ? c : p, 0)
            Object.keys(doc.delegations).forEach(function (k) {
                emit([k, latestUpdate], doc._id);
            });
        }
    }
}
