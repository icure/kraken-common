function(doc) {
    if (doc.java_type == 'org.taktik.icure.entities.Tarification' && !doc.deleted) {
        doc.regions.forEach(function (r) {
            emit([r, doc.type, doc.code, doc.version], doc._id);
        });
    }
}
