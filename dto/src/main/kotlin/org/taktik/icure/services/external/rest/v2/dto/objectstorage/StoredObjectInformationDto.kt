package org.taktik.icure.services.external.rest.v2.dto.objectstorage

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

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
	@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.objectstorage.StoredObjectInformationDto.AvailableDto")
	data class AvailableDto(
		@param:Schema(required = true)
		@ActiveField val md5HashHexString: String,
	) : StoredObjectInformationDto

	/**
	 * The object is currently getting stored.
	 * @param nextByte the next expected byte of the object content (all bytes up until the previous have already been stored).
	 * @param md5HashHexString md5 hash of the expected hash of the full content as an hex string.
	 */
	@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.objectstorage.StoredObjectInformationDto.StoringDto")
	data class StoringDto(
		@param:Schema(required = true)
		@ActiveField val nextByte: Long,
		@param:Schema(required = true)
		@ActiveField val md5HashHexString: String?,
	) : StoredObjectInformationDto

	@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.objectstorage.StoredObjectInformationDto.NotStoredDto")
	data object NotStoredDto : StoredObjectInformationDto
}
