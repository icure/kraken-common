function(doc) {
    if (doc.java_type === 'org.taktik.icure.entities.HealthElement' && !doc.deleted && doc.healthElementId != undefined) {
        let healthElementTime = doc.modified
        if (doc.endOfLife != null && (healthElementTime == null || doc.endOfLife > healthElementTime)) {
            healthElementTime = doc.endOfLife
        }
        if (doc.created != null && (healthElementTime == null || doc.created > healthElementTime)) {
            healthElementTime = doc.created
        }
        emit(doc.healthElementId, [healthElementTime, doc._id]);
    }
}
