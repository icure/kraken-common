function(doc) {
    if (doc.java_type === 'org.taktik.icure.entities.HealthElement' && !doc.deleted && doc.qualifiedLinks) {
        doc.qualifiedLinks.forEach(function (l) {
            if (l.associationId != null) {
                emit(l.associationId, null);
            }
        });
    }
}
