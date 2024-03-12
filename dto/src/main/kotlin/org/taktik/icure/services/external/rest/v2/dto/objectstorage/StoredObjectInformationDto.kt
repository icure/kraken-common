package org.taktik.icure.services.external.rest.v2.dto.objectstorage

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
	JsonSubTypes.Type(value = AvailableDto::class, name = "Available"),
	JsonSubTypes.Type(value = StoringDto::class, name = "Storing"),
	JsonSubTypes.Type(value = NotStoredDto::class, name = "NotStored"),
)
sealed interface StoredObjectInformationDto
