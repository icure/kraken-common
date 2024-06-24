package org.taktik.icure.services.external.rest.v2.dto.objectstorage

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.handlers.JsonDiscriminated

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
	JsonSubTypes.Type(value = StoredObjectInformationDto.AvailableDto::class, name = "Available"),
	JsonSubTypes.Type(value = StoredObjectInformationDto.StoringDto::class, name = "Storing"),
	JsonSubTypes.Type(value = StoredObjectInformationDto.NotStoredDto::class, name = "NotStored"),
)
sealed interface StoredObjectInformationDto {

	/**
	 * The object is fully stored and available.
	 * @param md5HashHexString hex string representation of the md5 hash of the content.
	 */
	data class AvailableDto(
		@Schema(required = true)
		val md5HashHexString: String
	) : StoredObjectInformationDto

	/**
	 * The object is currently getting stored.
	 * @param nextByte the next expected byte of the object content (all bytes up until the previous have already been stored).
	 * @param md5HashHexString md5 hash of the expected hash of the full content as an hex string.
	 */
	data class StoringDto(
		@Schema(required = true)
		val nextByte: Long,
		@Schema(required = true)
		val md5HashHexString: String?
	) : StoredObjectInformationDto

	data object NotStoredDto : StoredObjectInformationDto
}
