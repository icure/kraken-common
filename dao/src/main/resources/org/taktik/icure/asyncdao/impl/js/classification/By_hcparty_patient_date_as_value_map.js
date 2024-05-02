function(doc) {
    var emit_contacts = function(d,doc) {
        doc.secretForeignKeys.forEach(function(fk) {
            emit([d.delegatedTo, fk], doc.created);
        });
    };

    if (doc.java_type == 'org.taktik.icure.entities.Classification' && !doc.deleted && doc.secretForeignKeys && doc.secretForeignKeys.length && doc.delegations) {
        Object.keys(doc.delegations).forEach(function (k) {
            var ds = doc.delegations[k];

            ds.forEach(function (d) {
                emit_contacts(d,doc);
            });
        });
    }
}