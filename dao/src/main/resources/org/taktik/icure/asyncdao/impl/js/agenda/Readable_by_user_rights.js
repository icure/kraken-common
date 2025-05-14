function(doc) {
    if (doc.java_type === 'org.taktik.icure.entities.Agenda' && !doc.deleted) {
        Object.keys(doc.userRights).forEach(function(userId) {
            emit(userId, null);
        })
    }
}
