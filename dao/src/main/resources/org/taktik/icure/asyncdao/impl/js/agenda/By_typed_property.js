function(doc) {
    if (doc.java_type === 'org.taktik.icure.entities.Agenda' && !doc.deleted) {
        if (doc.properties != null) {
            doc.properties.forEach(function(property) {
                if (property.id != null && property.typedValue != null) {
                    if (property.typedValue.stringValue != null) {
                        emit([property.id, 's', property.typedValue.stringValue], null);
                    }
                    if (property.typedValue.booleanValue != null) {
                        emit([property.id, 'b', property.typedValue.booleanValue], null);
                    }
                    if (property.typedValue.integerValue != null) {
                        emit([property.id, 'i', property.typedValue.integerValue], null);
                    }
                    if (property.typedValue.doubleValue != null) {
                        emit([property.id, 'd', property.typedValue.doubleValue], null);
                    }
                }
            })
        }
    }
}
