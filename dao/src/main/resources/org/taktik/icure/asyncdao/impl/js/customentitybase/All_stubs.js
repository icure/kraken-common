function (doc) {
    if (doc.java_type === 'org.taktik.icure.entities.CustomEntityBase') {
        var value = {};
        for (var key in doc) {
            if (doc.hasOwnProperty(key) && key !== '_id' && key !== 'extensions') {
                value[key] = doc[key];
            }
        }
        emit(doc._id, value);
    }
}