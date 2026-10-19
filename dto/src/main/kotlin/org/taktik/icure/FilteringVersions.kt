package org.taktik.icure

object FilteringVersions {
	/**
	 * First version of Cardinal supporting generic (custom) entities.
	 *
	 * Starting from this version also the deleted attachments are not included anymore in the dto of entities with data
	 * attachments, and an empty list of deleted attachments in input is replaced with the stored value.
	 */
	const val GENERIC_ENTITIES_CARDINAL_MIN_VERSION = "3.0.0-PREVIEW.8" // TODO might need to change: before merging onto main check if the version needs to be updated
}
