function(doc) {
  var emit_forms = function (k, doc) {
    emit([k, doc.parent], doc._id);
  };

  if (doc.java_type == 'org.taktik.icure.entities.Form' && !doc.deleted && doc.parent && doc.delegations && Object.keys(doc.delegations).length) {
    Object.keys(doc.delegations).forEach(function (k) {
      emit_forms(k, doc);
    });
  }
}
