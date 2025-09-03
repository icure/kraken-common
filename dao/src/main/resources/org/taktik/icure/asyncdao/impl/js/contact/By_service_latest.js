function(doc) {
    if (doc.java_type === 'org.taktik.icure.entities.Contact' && !doc.deleted ) {
        doc.services.forEach(
            function (s) {
                if (s._id != null) {
                    let serviceTime = s.modified
                    if (s.endOfLife != null && (serviceTime == null || s.endOfLife > serviceTime)) {
                        serviceTime = s.endOfLife
                    }
                    if (s.created != null && (serviceTime == null || s.created > serviceTime)) {
                        serviceTime = s.created
                    }
                    emit(s._id, [serviceTime, doc.modified, doc._id]);
                }
            }
        );
    }
}
