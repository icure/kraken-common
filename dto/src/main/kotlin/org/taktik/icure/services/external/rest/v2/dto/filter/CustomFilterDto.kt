package org.taktik.icure.services.external.rest.v2.dto.filter

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	property = "type",
)
@JsonSubTypes(
	value = [
		JsonSubTypes.Type(value = GenericByKeysCustomFilterDto::class, name = "byKeys"),
		JsonSubTypes.Type(value = GenericByRangeCustomFilterDto::class, name = "byRange"),
	],
)
/**
 * Payload accepted by a single-entity custom-filtering endpoint: either a by-keys or a
 * by-range view query. The endpoint for a given entity X maps this to the concrete
 * ByKeys/ByRange custom filter domain class for X.
 */
sealed interface CustomFilterDto
