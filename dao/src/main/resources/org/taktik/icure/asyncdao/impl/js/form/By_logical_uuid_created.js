function(doc) {
    if (doc.java_type == 'org.taktik.icure.entities.Form' && !doc.deleted && doc.logicalUuid)
        emit( [doc.logicalUuid, !!doc.created ? doc.created : 0], doc._id )
}