map = function (doc) {
    if (doc.java_type == 'org.taktik.icure.entities.HealthcareParty' && !doc.deleted) {
        var emitted = {};
        if (doc.parentId) {
            emitted[doc.parentId] = true;
            emit(doc.parentId, null);
        }
        if (doc.dataOwnerGroups) {
            for (var i = 0; i < doc.dataOwnerGroups.length; i++) {
                var link = doc.dataOwnerGroups[i];
                if (link && link.dataOwnerId && link.linkType === 'parent' && !emitted[link.dataOwnerId]) {
                    emitted[link.dataOwnerId] = true;
                    emit(link.dataOwnerId, null);
                }
            }
        }
    }
}
