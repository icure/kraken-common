package org.taktik.icure.services.external.rest.v1.dto.objectstorage

/**
 * The object is currently getting stored.
 * @param nextByte the next expected byte of the object content (all bytes up until the previous have already been stored).
 * @param md5HashHexString md5 hash of the expected hash of the full content as an hex string.
 */
data class StoringDto(val nextByte: Long, val md5HashHexString: String?) : StoredObjectInformationDto