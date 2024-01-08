function(doc) {
    if (doc.java_type === 'org.taktik.icure.entities.Patient' && !doc.deleted) {
        if (doc.delegations) {
            Object.keys(doc.delegations).forEach(function(k){
                emit([k, doc.gender, doc.education, doc.profession], doc._id);
            });
        }
    }
}
