package org.taktik.icure.services.external.rest.v1.dto.objectstorage

/**
 * The object is fully stored and available.
 * @param md5HashHexString hex string representation of the md5 hash of the content.
 */
data class AvailableDto(
	val md5HashHexString: String,
) : StoredObjectInformationDto
