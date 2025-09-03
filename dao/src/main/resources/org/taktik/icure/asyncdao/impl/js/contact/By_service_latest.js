function(doc) {
    if (doc.java_type === 'org.taktik.icure.entities.Contact' && !doc.deleted ) {
        doc.services.forEach(
            function (s) {
                if (s._id != null) {
                    const serviceTime = s.deleted && (s.modified == null || s.deleted > s.modified) ? s.deleted : s.modified
                    emit(s._id, [serviceTime, doc.modified, doc._id]);
                }
            }
        );
    }
}
