function(doc) {
    if (doc.java_type === 'org.taktik.icure.entities.Agenda' && !doc.deleted) {
        const emittedUsers = new Set();
        doc.rights.forEach(function(right) {
            if (!emittedUsers.has(right)) {
                emittedUsers.add(right);
                emit(right.userId, null);
            }
        });
        Object.keys(doc.userRights).forEach(function(userId) {
            if (!emittedUsers.has(userId)) {
                emittedUsers.add(userId);
                emit(userId, null);
            }
        })
    }
}