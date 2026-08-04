function(doc) {
    if (doc.java_type === 'org.taktik.icure.entities.HealthElement' && !doc.deleted && doc.qualifiedLinks) {
        doc.qualifiedLinks.forEach(function (l) {
            emit(l.healthElementId, l.type);
        });
    }
}
