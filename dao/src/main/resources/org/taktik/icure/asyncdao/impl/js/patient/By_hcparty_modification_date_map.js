function(doc) {
    if (doc.java_type == 'org.taktik.icure.entities.Patient' && !doc.deleted && (!!doc.modified || !!doc.created)) {
        if(doc.delegations) {
            Object.keys(doc.delegations).forEach(function(k){
                emit(k, doc.modified || doc.created);
            });
        }
    }
}
