package org.taktik.icure.exceptions

class RevisionNotAccessibleForMergeException(entityId: String, entityRev: String?) :
	Exception("You do not have write access to revision $entityRev of entity $entityId and you cannot merge it") {

	companion object {
		const val EXCEPTION_DETAIL = "RevisionNotAccessibleForMergeException"
	}
}