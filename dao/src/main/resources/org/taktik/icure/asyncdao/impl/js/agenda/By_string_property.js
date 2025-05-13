function(doc) {
    if (doc.java_type === 'org.taktik.icure.entities.Agenda' && !doc.deleted) {
        doc.properties.forEach(function(property) {
            if (property.id != null && property.typedValue != null && property.typedValue.stringValue != null) {
                emit([property.id, property.typedValue.stringValue], null);
            }
        })
    }
}
