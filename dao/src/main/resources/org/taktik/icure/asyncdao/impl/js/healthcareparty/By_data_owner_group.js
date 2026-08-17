map = function (doc) {
    if (doc.java_type == 'org.taktik.icure.entities.HealthcareParty' && !doc.deleted) {
        // The emitted value is this healthcare party's own groupLinkType, that is the type of any link pointing
        // at *it*, not at the group it declares here: it is what a caller listing the members of a group needs to
        // decide whether to recurse into a member, and emitting it here saves a second request. It is null when
        // this healthcare party relies on the default for its data owner type.
        var linkType = doc.groupLinkType != null ? doc.groupLinkType : null;
        var emitted = {};
        if (doc.parentId) {
            emitted[doc.parentId] = true;
            emit(doc.parentId, linkType);
        }
        if (doc.dataOwnerGroups) {
            for (var i = 0; i < doc.dataOwnerGroups.length; i++) {
                var link = doc.dataOwnerGroups[i];
                if (link && link.dataOwnerId && !emitted[link.dataOwnerId]) {
                    emitted[link.dataOwnerId] = true;
                    emit(link.dataOwnerId, linkType);
                }
            }
        }
    }
}
